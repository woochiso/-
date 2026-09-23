package com.example.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.example.BuildConfig
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Creates analysis-only PCM WAV files without changing the user's source recording. */
object VocalWavPreparer {
    private const val TARGET_RATE = 16_000
    private const val TAG = "VocalAnalysis"

    data class Prepared(
        val file: File,
        val decodedSampleRate: Int,
        val decodedChannels: Int,
        val wavFrames: Int
    )

    class PreparationException(val code: String, cause: Throwable? = null) : Exception(code, cause)

    fun prepare(source: File, cacheDir: File, label: String = "vocal", maxDurationSeconds: Double? = null): Prepared {
        require(source.isFile && source.length() > 0) { "Missing audio source" }
        return try {
            val decoded = decodeToMono(source, maxDurationSeconds)
            val resampled = resample(decoded.samples, decoded.sampleRate, TARGET_RATE)
            if (resampled.isEmpty()) throw PreparationException("AUDIO_RESAMPLE_FAILED")
            val output = File.createTempFile("$label-analysis-", ".wav", cacheDir)
            try {
                writePcm16Wav(output, resampled, TARGET_RATE)
            } catch (error: Throwable) {
                output.delete()
                throw PreparationException("WAV_CREATE_FAILED", error)
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "source_format=${source.extension.lowercase()} source_size=${source.length()}")
                Log.d(TAG, "decoded_sample_rate=${decoded.sampleRate} decoded_channels=${decoded.channels}")
                Log.d(TAG, "wav_sample_rate=$TARGET_RATE wav_channels=1 wav_bits=16 wav_size=${output.length()}")
            }
            Prepared(output, decoded.sampleRate, decoded.channels, resampled.size)
        } catch (error: PreparationException) {
            if (BuildConfig.DEBUG) Log.e(TAG, "prepare_failed code=${error.code}", error)
            throw error
        } catch (error: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "prepare_failed code=AUDIO_DECODE_FAILED", error)
            throw PreparationException("AUDIO_DECODE_FAILED", error)
        }
    }

    fun createMix(vocal: Prepared, mr: Prepared, cacheDir: File): File {
        return try {
            val vocalSamples = readPcm16Wav(vocal.file)
            val mrSamples = readPcm16Wav(mr.file)
            val mixed = ShortArray(vocalSamples.size) { index ->
                val voice = vocalSamples[index].toInt() * 0.65f
                val backing = (mrSamples.getOrNull(index)?.toInt() ?: 0) * 0.35f
                (voice + backing).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            File.createTempFile("vocal-mix-", ".wav", cacheDir).also {
                writePcm16Wav(it, mixed, TARGET_RATE)
                if (BuildConfig.DEBUG) Log.d(TAG, "mix_wav_size=${it.length()} mix_frames=${mixed.size}")
            }
        } catch (error: Throwable) {
            if (BuildConfig.DEBUG) Log.e(TAG, "prepare_failed code=WAV_CREATE_FAILED stage=mix", error)
            throw PreparationException("WAV_CREATE_FAILED", error)
        }
    }

    private data class Decoded(val samples: FloatArray, val sampleRate: Int, val channels: Int)

    private fun decodeToMono(source: File, maxDurationSeconds: Double?): Decoded {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw PreparationException("AUDIO_DECODE_FAILED")
            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw PreparationException("AUDIO_DECODE_FAILED")
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            var sampleRate = inputFormat.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, 0)
            var channels = inputFormat.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, 0)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            val accumulator = FloatAccumulator()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var idleCount = 0

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex) ?: throw PreparationException("AUDIO_DECODE_FAILED")
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime.coerceAtLeast(0), 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        sampleRate = outputFormat.getIntegerOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                        channels = outputFormat.getIntegerOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                        pcmEncoding = outputFormat.getIntegerOr(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        idleCount++
                        if (inputEnded && idleCount > 500) throw PreparationException("AUDIO_DECODE_FAILED")
                    }
                    else -> if (outputIndex >= 0) {
                        idleCount = 0
                        val output = codec.getOutputBuffer(outputIndex)
                        if (output != null && info.size > 0) {
                            output.order(ByteOrder.LITTLE_ENDIAN)
                            output.position(info.offset)
                            output.limit(info.offset + info.size)
                            appendMono(output, pcmEncoding, channels.coerceAtLeast(1), accumulator)
                        }
                        val durationLimitReached = maxDurationSeconds != null && sampleRate > 0 &&
                            accumulator.size >= (sampleRate * maxDurationSeconds).roundToInt()
                        outputEnded = durationLimitReached || info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
            if (sampleRate <= 0 || channels <= 0 || accumulator.size == 0) throw PreparationException("AUDIO_DECODE_FAILED")
            return Decoded(accumulator.toArray(), sampleRate, channels)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun appendMono(buffer: java.nio.ByteBuffer, encoding: Int, channels: Int, output: FloatAccumulator) {
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> while (buffer.remaining() >= channels * 4) {
                var sum = 0f
                repeat(channels) { sum += buffer.float }
                output.add((sum / channels).coerceIn(-1f, 1f))
            }
            AudioFormat.ENCODING_PCM_8BIT -> while (buffer.remaining() >= channels) {
                var sum = 0f
                repeat(channels) { sum += ((buffer.get().toInt() and 0xff) - 128) / 128f }
                output.add((sum / channels).coerceIn(-1f, 1f))
            }
            else -> while (buffer.remaining() >= channels * 2) {
                var sum = 0f
                repeat(channels) { sum += buffer.short / 32768f }
                output.add((sum / channels).coerceIn(-1f, 1f))
            }
        }
    }

    private fun resample(input: FloatArray, sourceRate: Int, targetRate: Int): ShortArray {
        if (input.isEmpty() || sourceRate <= 0) return ShortArray(0)
        val frames = (input.size.toDouble() * targetRate / sourceRate).roundToInt().coerceAtLeast(1)
        val ratio = sourceRate.toDouble() / targetRate
        return ShortArray(frames) { index ->
            val position = index * ratio
            val lower = position.toInt().coerceIn(0, input.lastIndex)
            val upper = (lower + 1).coerceAtMost(input.lastIndex)
            val fraction = (position - lower).toFloat()
            val sample = (input[lower] + (input[upper] - input[lower]) * fraction).coerceIn(-1f, 1f)
            (sample * if (sample < 0f) 32768f else 32767f).roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun writePcm16Wav(file: File, samples: ShortArray, sampleRate: Int) {
        DataOutputStream(BufferedOutputStream(FileOutputStream(file))).use { out ->
            val dataBytes = samples.size * 2
            out.writeBytes("RIFF"); out.writeLeInt(36 + dataBytes); out.writeBytes("WAVE")
            out.writeBytes("fmt "); out.writeLeInt(16); out.writeLeShort(1); out.writeLeShort(1)
            out.writeLeInt(sampleRate); out.writeLeInt(sampleRate * 2); out.writeLeShort(2); out.writeLeShort(16)
            out.writeBytes("data"); out.writeLeInt(dataBytes)
            samples.forEach { out.writeLeShort(it.toInt()) }
        }
    }

    private fun readPcm16Wav(file: File): ShortArray {
        val bytes = file.readBytes()
        if (bytes.size < 44 || String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") {
            throw PreparationException("WAV_CREATE_FAILED")
        }
        return ShortArray((bytes.size - 44) / 2) { index ->
            val offset = 44 + index * 2
            ((bytes[offset].toInt() and 0xff) or (bytes[offset + 1].toInt() shl 8)).toShort()
        }
    }

    private fun MediaFormat.getIntegerOr(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback

    private fun DataOutputStream.writeLeInt(value: Int) {
        writeByte(value); writeByte(value ushr 8); writeByte(value ushr 16); writeByte(value ushr 24)
    }

    private fun DataOutputStream.writeLeShort(value: Int) {
        writeByte(value); writeByte(value ushr 8)
    }

    private class FloatAccumulator(initialCapacity: Int = 65_536) {
        private var values = FloatArray(initialCapacity)
        var size: Int = 0
            private set
        fun add(value: Float) {
            if (size == values.size) values = values.copyOf(values.size * 2)
            values[size++] = value
        }
        fun toArray(): FloatArray = values.copyOf(size)
    }
}

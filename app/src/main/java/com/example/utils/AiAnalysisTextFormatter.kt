package com.example.utils

/** 신규 응답뿐 아니라 이미 저장된 예전 캐시에서도 개발용 키가 노출되지 않게 하는 표시 계층. */
object AiAnalysisTextFormatter {
    private val count = Regex("same_direction_count\\s*[:=]?\\s*(\\d+)", RegexOption.IGNORE_CASE)
    private val ratio = Regex("same_direction_ratio\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)%?", RegexOption.IGNORE_CASE)
    private val repeated = Regex("repeated_change\\s*[:=]?\\s*(true|false)", RegexOption.IGNORE_CASE)
    private val unknownKey = Regex("\\b[a-z]+(?:_[a-z0-9]+)+\\b\\s*[:=]?\\s*(?:true|false|[-+]?\\d+(?:\\.\\d+)?%?)?", RegexOption.IGNORE_CASE)

    fun normalize(raw: String): String = raw
        .replace("**", "")
        .replace(count) { "같은 방향 변화 ${it.groupValues[1]}회" }
        .replace(ratio) { "같은 방향으로 변화한 비율 ${it.groupValues[1]}%" }
        .replace(repeated) { if (it.groupValues[1].equals("true", true)) "반복되는 변화가 관찰되었습니다" else "반복 여부를 더 살펴봐야 합니다" }
        .replace(unknownKey, "")
        .replace(Regex("[ \\t]+([,.)])"), "$1")
        .replace(Regex("[ \\t]{2,}"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    fun sections(raw: String): List<Pair<String, String>> {
        val clean = normalize(raw)
        val headings = listOf("① 관찰된 변화", "② 반복 패턴", "③ 기록을 이어갈 때 살펴볼 점")
        val positions = headings.map(clean::indexOf)
        if (positions.any { it < 0 }) return listOf("AI 분석" to clean)
        return headings.mapIndexed { index, heading ->
            val start = positions[index] + heading.length
            val end = positions.getOrNull(index + 1) ?: clean.length
            heading to clean.substring(start, end).trim().trimStart(':').trim()
        }
    }

    fun paragraphs(body: String): List<String> = body
        .split(Regex("\\n\\s*\\n|(?<=[.!?])\\s+(?=[가-힣①②③])"))
        .map(String::trim)
        .filter(String::isNotBlank)
}

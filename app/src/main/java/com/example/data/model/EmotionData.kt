package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class EmotionCategory(
    val code: String,
    val hanja: String,
    val koreanLabel: String,
    val colorHex: Long,
    val description: String
) {
    JOY(
        code = "JOY",
        hanja = "희(喜)",
        koreanLabel = "기쁨",
        colorHex = 0xFFFF9F1C, // Cheerful Coral Amber
        description = "마음이 밝고 흐뭇하며 유쾌한 감정"
    ),
    ANGER(
        code = "ANGER",
        hanja = "노(怒)",
        koreanLabel = "노여움",
        colorHex = 0xFFFF4757, // Vivid Rose Red
        description = "분하고 서운하거나 불쾌하여 솟구치는 감정"
    ),
    SORROW(
        code = "SORROW",
        hanja = "애(哀)",
        koreanLabel = "슬픔",
        colorHex = 0xFF1E90FF, // Luminous Sapphire Azure
        description = "마음이 아프고 눈물이 나며 허전한 감정"
    ),
    PLEASURE(
        code = "PLEASURE",
        hanja = "락(樂)",
        koreanLabel = "즐거움",
        colorHex = 0xFF2ED573, // Vibrant Lime Mint
        description = "신나고 활기차며 생동감 넘치는 감정"
    ),
    LOVE(
        code = "LOVE",
        hanja = "애(愛)",
        koreanLabel = "사랑",
        colorHex = 0xFFFF6B81, // Vibrant Blossom Pink
        description = "따뜻하고 아끼며 뭉클하게 전해지는 감정"
    ),
    HATRED(
        code = "HATRED",
        hanja = "오(惡)",
        koreanLabel = "미움",
        colorHex = 0xFF8B5CF6, // Bright Royal Violet
        description = "싫고 거부감이 들며 안타까운 감정"
    ),
    DESIRE(
        code = "DESIRE",
        hanja = "욕(慾)",
        koreanLabel = "바라다",
        colorHex = 0xFF00CEC9, // Vibrant Electric Turquoise
        description = "간절히 원하고 기대하며 소망하는 감정"
    );

    val color: Color
        get() = Color(colorHex)

    companion object {
        fun fromCode(code: String): EmotionCategory {
            return entries.find { it.code == code } ?: JOY
        }

        fun fromKoreanLabel(label: String): EmotionCategory {
            return entries.find { it.koreanLabel == label || it.hanja.contains(label) } ?: JOY
        }
    }
}

data class EmotionWordItem(
    val word: String,
    val category: EmotionCategory,
    val isFavorite: Boolean = false
)

object PresetEmotions {
    val ALL_EMOTIONS: List<EmotionWordItem> = listOf(
        // 희 - 기쁨
        EmotionWordItem("감격스러운", EmotionCategory.JOY),
        EmotionWordItem("감동적인", EmotionCategory.JOY),
        EmotionWordItem("감사한", EmotionCategory.JOY),
        EmotionWordItem("고마운", EmotionCategory.JOY),
        EmotionWordItem("고무적인", EmotionCategory.JOY),
        EmotionWordItem("기쁜", EmotionCategory.JOY),
        EmotionWordItem("고전적인", EmotionCategory.JOY),
        EmotionWordItem("날아갈듯한", EmotionCategory.JOY),
        EmotionWordItem("놀라운", EmotionCategory.JOY),
        EmotionWordItem("가벼운", EmotionCategory.JOY),
        EmotionWordItem("눈물겨운", EmotionCategory.JOY),
        EmotionWordItem("든든한", EmotionCategory.JOY),
        EmotionWordItem("만족스러운", EmotionCategory.JOY),
        EmotionWordItem("뭉클한", EmotionCategory.JOY),
        EmotionWordItem("반가운", EmotionCategory.JOY),
        EmotionWordItem("벅찬", EmotionCategory.JOY),
        EmotionWordItem("뿌듯한", EmotionCategory.JOY),
        EmotionWordItem("살맛나는", EmotionCategory.JOY),
        EmotionWordItem("시원한", EmotionCategory.JOY),
        EmotionWordItem("싱그러은", EmotionCategory.JOY),
        EmotionWordItem("좋은", EmotionCategory.JOY),
        EmotionWordItem("짜릿한", EmotionCategory.JOY),
        EmotionWordItem("쾌적한", EmotionCategory.JOY),
        EmotionWordItem("통쾌한", EmotionCategory.JOY),
        EmotionWordItem("포근한", EmotionCategory.JOY),
        EmotionWordItem("푸근한", EmotionCategory.JOY),
        EmotionWordItem("행복한", EmotionCategory.JOY),
        EmotionWordItem("환상적인", EmotionCategory.JOY),
        EmotionWordItem("후련한", EmotionCategory.JOY),
        EmotionWordItem("흐웃한", EmotionCategory.JOY),
        EmotionWordItem("흔쾌한", EmotionCategory.JOY),
        EmotionWordItem("흥분된", EmotionCategory.JOY),

        // 노 - 노여움
        EmotionWordItem("가혹한", EmotionCategory.ANGER),
        EmotionWordItem("고통스러운", EmotionCategory.ANGER),
        EmotionWordItem("골치아픈", EmotionCategory.ANGER),
        EmotionWordItem("괘씸한", EmotionCategory.ANGER),
        EmotionWordItem("구역질 나는", EmotionCategory.ANGER),
        EmotionWordItem("기분이 상한는", EmotionCategory.ANGER),
        EmotionWordItem("꼴사나운", EmotionCategory.ANGER),
        EmotionWordItem("끓어오르는", EmotionCategory.ANGER),
        EmotionWordItem("나쁜", EmotionCategory.ANGER),
        EmotionWordItem("노한", EmotionCategory.ANGER),
        EmotionWordItem("떫은", EmotionCategory.ANGER),
        EmotionWordItem("모욕적", EmotionCategory.ANGER),
        EmotionWordItem("무서운", EmotionCategory.ANGER),
        EmotionWordItem("배반감", EmotionCategory.ANGER),
        EmotionWordItem("복수심", EmotionCategory.ANGER),
        EmotionWordItem("북받침", EmotionCategory.ANGER),
        EmotionWordItem("분개한", EmotionCategory.ANGER),
        EmotionWordItem("분노", EmotionCategory.ANGER),
        EmotionWordItem("불만스러운", EmotionCategory.ANGER),
        EmotionWordItem("불쾌한", EmotionCategory.ANGER),
        EmotionWordItem("섬짓한", EmotionCategory.ANGER),
        EmotionWordItem("소름 끼치는", EmotionCategory.ANGER),
        EmotionWordItem("속상한", EmotionCategory.ANGER),
        EmotionWordItem("숨막히는", EmotionCategory.ANGER),
        EmotionWordItem("실망감", EmotionCategory.ANGER),
        EmotionWordItem("쓰라린", EmotionCategory.ANGER),
        EmotionWordItem("씁쓸한", EmotionCategory.ANGER),
        EmotionWordItem("약오르는", EmotionCategory.ANGER),

        // 애 - 슬픔
        EmotionWordItem("가슴 아픈", EmotionCategory.SORROW),
        EmotionWordItem("걱정되는", EmotionCategory.SORROW),
        EmotionWordItem("고단한", EmotionCategory.SORROW),
        EmotionWordItem("고독한", EmotionCategory.SORROW),
        EmotionWordItem("고민스러운", EmotionCategory.SORROW),
        EmotionWordItem("공포에 질린", EmotionCategory.SORROW),
        EmotionWordItem("공허한", EmotionCategory.SORROW),
        EmotionWordItem("괴로운", EmotionCategory.SORROW),
        EmotionWordItem("구슬픈", EmotionCategory.SORROW),
        EmotionWordItem("권태로운", EmotionCategory.SORROW),
        EmotionWordItem("근심되는", EmotionCategory.SORROW),
        EmotionWordItem("기분나쁜", EmotionCategory.SORROW),
        EmotionWordItem("낙담한", EmotionCategory.SORROW),
        EmotionWordItem("두려운", EmotionCategory.SORROW),
        EmotionWordItem("마음이 무거운", EmotionCategory.SORROW),
        EmotionWordItem("멍한", EmotionCategory.SORROW),
        EmotionWordItem("미어지는", EmotionCategory.SORROW),
        EmotionWordItem("부끄러운", EmotionCategory.SORROW),
        EmotionWordItem("불쌍한", EmotionCategory.SORROW),
        EmotionWordItem("불안한", EmotionCategory.SORROW),
        EmotionWordItem("불편한", EmotionCategory.SORROW),
        EmotionWordItem("비참한", EmotionCategory.SORROW),
        EmotionWordItem("비탄함", EmotionCategory.SORROW),
        EmotionWordItem("서글픈", EmotionCategory.SORROW),
        EmotionWordItem("암담한", EmotionCategory.SORROW),
        EmotionWordItem("앞이 깜깜한", EmotionCategory.SORROW),
        EmotionWordItem("애석한", EmotionCategory.SORROW),
        EmotionWordItem("애처로운", EmotionCategory.SORROW),
        EmotionWordItem("애태우는", EmotionCategory.SORROW),
        EmotionWordItem("애통한", EmotionCategory.SORROW),
        EmotionWordItem("언짠은", EmotionCategory.SORROW),
        EmotionWordItem("염려하는", EmotionCategory.SORROW),
        EmotionWordItem("외로운", EmotionCategory.SORROW),
        EmotionWordItem("우울한", EmotionCategory.SORROW),
        EmotionWordItem("울적한", EmotionCategory.SORROW),
        EmotionWordItem("음울한", EmotionCategory.SORROW),
        EmotionWordItem("음친한", EmotionCategory.SORROW),
        EmotionWordItem("의기소침한", EmotionCategory.SORROW),
        EmotionWordItem("절망적인", EmotionCategory.SORROW),
        EmotionWordItem("좌절하는", EmotionCategory.SORROW),
        EmotionWordItem("증오하는", EmotionCategory.SORROW),
        EmotionWordItem("지루한", EmotionCategory.SORROW),
        EmotionWordItem("찹찹한", EmotionCategory.SORROW),
        EmotionWordItem("참담한", EmotionCategory.SORROW),
        EmotionWordItem("창피한", EmotionCategory.SORROW),
        EmotionWordItem("처량한", EmotionCategory.SORROW),
        EmotionWordItem("처참한", EmotionCategory.SORROW),
        EmotionWordItem("측은한", EmotionCategory.SORROW),
        EmotionWordItem("침통한", EmotionCategory.SORROW),
        EmotionWordItem("패배스러운", EmotionCategory.SORROW),
        EmotionWordItem("한스러운", EmotionCategory.SORROW),
        EmotionWordItem("허전한", EmotionCategory.SORROW),
        EmotionWordItem("허탈한", EmotionCategory.SORROW),
        EmotionWordItem("허한", EmotionCategory.SORROW),
        EmotionWordItem("황량한", EmotionCategory.SORROW),

        // 락 - 즐거움
        EmotionWordItem("가뿐한", EmotionCategory.PLEASURE),
        EmotionWordItem("경쾌한", EmotionCategory.PLEASURE),
        EmotionWordItem("고요한", EmotionCategory.PLEASURE),
        EmotionWordItem("기분좋은", EmotionCategory.PLEASURE),
        EmotionWordItem("담담한", EmotionCategory.PLEASURE),
        EmotionWordItem("명랑한", EmotionCategory.PLEASURE),
        EmotionWordItem("밝은", EmotionCategory.PLEASURE),
        EmotionWordItem("산뜻한", EmotionCategory.PLEASURE),
        EmotionWordItem("상쾌한", EmotionCategory.PLEASURE),
        EmotionWordItem("상큼한", EmotionCategory.PLEASURE),
        EmotionWordItem("숨가뿐", EmotionCategory.PLEASURE),
        EmotionWordItem("신나는", EmotionCategory.PLEASURE),
        EmotionWordItem("유쾌한", EmotionCategory.PLEASURE),
        EmotionWordItem("자신 있는", EmotionCategory.PLEASURE),
        EmotionWordItem("즐거운", EmotionCategory.PLEASURE),
        EmotionWordItem("쾌활한", EmotionCategory.PLEASURE),
        EmotionWordItem("편안한", EmotionCategory.PLEASURE),
        EmotionWordItem("홀가분한", EmotionCategory.PLEASURE),
        EmotionWordItem("활기 있는", EmotionCategory.PLEASURE),
        EmotionWordItem("활발한", EmotionCategory.PLEASURE),
        EmotionWordItem("흐뭇한", EmotionCategory.PLEASURE),
        EmotionWordItem("희망찬", EmotionCategory.PLEASURE),

        // 애 - 사랑
        EmotionWordItem("감미로운", EmotionCategory.LOVE),
        EmotionWordItem("감사하는", EmotionCategory.LOVE),
        EmotionWordItem("그리운", EmotionCategory.LOVE),
        EmotionWordItem("다정한", EmotionCategory.LOVE),
        EmotionWordItem("따사로운", EmotionCategory.LOVE),
        EmotionWordItem("묘한", EmotionCategory.LOVE),
        EmotionWordItem("사랑스러운", EmotionCategory.LOVE),
        EmotionWordItem("상냥한", EmotionCategory.LOVE),
        EmotionWordItem("순수한", EmotionCategory.LOVE),
        EmotionWordItem("애뜻한", EmotionCategory.LOVE),
        EmotionWordItem("열렬한", EmotionCategory.LOVE),
        EmotionWordItem("열망하는", EmotionCategory.LOVE),
        EmotionWordItem("친숙한", EmotionCategory.LOVE),
        EmotionWordItem("호감이 가는", EmotionCategory.LOVE),
        EmotionWordItem("화끈거리는", EmotionCategory.LOVE),
        EmotionWordItem("흡족한", EmotionCategory.LOVE),

        // 오 - 미움
        EmotionWordItem("귀찮은", EmotionCategory.HATRED),
        EmotionWordItem("근심스러운", EmotionCategory.HATRED),
        EmotionWordItem("끔직한", EmotionCategory.HATRED),
        EmotionWordItem("모서리치는", EmotionCategory.HATRED),
        EmotionWordItem("무정한", EmotionCategory.HATRED),
        EmotionWordItem("미운", EmotionCategory.HATRED),
        EmotionWordItem("부담스런", EmotionCategory.HATRED),
        EmotionWordItem("서운한", EmotionCategory.HATRED),
        EmotionWordItem("싫은", EmotionCategory.HATRED),
        EmotionWordItem("싫증나는", EmotionCategory.HATRED),
        EmotionWordItem("쌀쌀한", EmotionCategory.HATRED),
        EmotionWordItem("야속한", EmotionCategory.HATRED),
        EmotionWordItem("얄미운", EmotionCategory.HATRED),
        EmotionWordItem("억울한", EmotionCategory.HATRED),
        EmotionWordItem("원망스러운", EmotionCategory.HATRED),
        EmotionWordItem("죄스런", EmotionCategory.HATRED),
        EmotionWordItem("죄책감", EmotionCategory.HATRED),
        EmotionWordItem("증오스러운", EmotionCategory.HATRED),
        EmotionWordItem("지겨운", EmotionCategory.HATRED),
        EmotionWordItem("짜증스러운", EmotionCategory.HATRED),
        EmotionWordItem("차가운", EmotionCategory.HATRED),

        // 욕 - 바라다
        EmotionWordItem("간절한", EmotionCategory.DESIRE),
        EmotionWordItem("갈망하는", EmotionCategory.DESIRE),
        EmotionWordItem("기대하는", EmotionCategory.DESIRE),
        EmotionWordItem("바라는", EmotionCategory.DESIRE),
        EmotionWordItem("소망하는", EmotionCategory.DESIRE),
        EmotionWordItem("애끊는", EmotionCategory.DESIRE),
        EmotionWordItem("절박한", EmotionCategory.DESIRE),
        EmotionWordItem("찝찝한", EmotionCategory.DESIRE),
        EmotionWordItem("초라한", EmotionCategory.DESIRE),
        EmotionWordItem("초조한", EmotionCategory.DESIRE),
        EmotionWordItem("호기심", EmotionCategory.DESIRE),
        EmotionWordItem("후회스런", EmotionCategory.DESIRE),
        EmotionWordItem("희망하는", EmotionCategory.DESIRE)
    )
}

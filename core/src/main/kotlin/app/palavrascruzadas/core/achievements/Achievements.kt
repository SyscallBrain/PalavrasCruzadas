package app.palavrascruzadas.core.achievements

import kotlinx.serialization.Serializable

/** Numbers the achievements read. Built from the progress and the statistics. */
@Serializable
data class AchievementStats(
    val puzzlesDone: Int = 0,
    val wordsSolved: Int = 0,
    val levelsDone: Int = 0,
    /** Best star total of a single world (40 levels, at most 120). */
    val bestWorldStars: Int = 0,
    val worldsDone: Int = 0,
    val threeStarLevels: Int = 0,
    val dailyMaxStreak: Int = 0,
    val bossWithoutHints: Int = 0,
    val miniBestSeconds: Int? = null,
    val timeAttackBest: Int? = null,
    val keywordsGuessed: Int = 0,
    /** Keywords guessed with at least half of their letters already correct. */
    val keywordsHalfKnown: Int = 0,
    val wheelsFullBonus: Int = 0,
    val booksDone: Int = 0,
    val modesUsed: Set<String> = emptySet(),
    val visualThemesUsed: Set<String> = emptySet(),
    val freeGridSizesDone: Set<Int> = emptySet(),
) {
    companion object {
        /** Game modes counted for "One of each mode". */
        val MODES = setOf(
            "campanha", "diario", "livre", "mini", "caderno", "contra-relogio",
            "palavra-chave", "roda", "diretas", "encaixar",
        )
    }
}

/** An achievement: stable id, on-screen text and its rule. */
enum class Achievement(val id: String, val title: String, val description: String, val rule: (AchievementStats) -> Boolean) {
    FIRST_GRID("primeira-grelha", "Primeira grelha", "Concluir um puzzle", { it.puzzlesDone >= 1 }),
    CAMPAIGN_10("campanha-10", "Dez níveis", "Concluir 10 níveis da campanha", { it.levelsDone >= 10 }),
    CAMPAIGN_100("campanha-100", "Cem níveis", "Concluir 100 níveis da campanha", { it.levelsDone >= 100 }),
    CAMPAIGN_600("campanha-600", "Campanha completa", "Concluir os 600 níveis", { it.levelsDone >= 600 }),
    PERFECT_WORLD("mundo-120", "Mundo perfeito", "Um mundo com 120 estrelas (40 níveis de 3)", { it.bestWorldStars >= 120 }),
    ONE_WORLD("mundo-1", "Primeiro mundo", "Concluir um mundo inteiro", { it.worldsDone >= 1 }),
    FIVE_WORLDS("mundo-5", "Cinco mundos", "Concluir cinco mundos", { it.worldsDone >= 5 }),
    THREE_STARS_10("tres-estrelas-10", "Dez de três", "10 níveis com 3 estrelas", { it.threeStarLevels >= 10 }),
    THREE_STARS_100("tres-estrelas-100", "Cem de três", "100 níveis com 3 estrelas", { it.threeStarLevels >= 100 }),
    DAILY_7("diario-7", "Uma semana", "7 dias seguidos no diário", { it.dailyMaxStreak >= 7 }),
    DAILY_30("diario-30", "Um mês", "30 dias seguidos no diário", { it.dailyMaxStreak >= 30 }),
    DAILY_100("diario-100", "Cem dias", "100 dias seguidos no diário", { it.dailyMaxStreak >= 100 }),
    BOSS_NO_HINTS("chefe-sem-ajudas", "Chefe limpo", "Vencer um chefe sem ajudas", { it.bossWithoutHints >= 1 }),
    MINI_60("mini-60", "Mini veloz", "Mini rápido em menos de 60 s", { (it.miniBestSeconds ?: Int.MAX_VALUE) < 60 }),
    TIME_ATTACK_25("contra-25", "Vinte e cinco", "25 palavras num contra-relógio", { (it.timeAttackBest ?: 0) >= 25 }),
    TIME_ATTACK_10("contra-10", "Relógio aquecido", "10 palavras num contra-relógio", { (it.timeAttackBest ?: 0) >= 10 }),
    KEYWORD_1("palavra-chave-1", "Chave certa", "Adivinhar uma palavra-chave", { it.keywordsGuessed >= 1 }),
    KEYWORD_HALF("palavra-chave-metade", "Quase sem ajuda", "Adivinhar uma palavra-chave com metade das letras já certas", { it.keywordsHalfKnown >= 1 }),
    WHEEL_BONUS("roda-bonus", "Roda cheia", "Todas as palavras bónus de uma roda", { it.wheelsFullBonus >= 1 }),
    BOOK_COMPLETE("caderno-completo", "Caderno completo", "Concluir um caderno inteiro", { it.booksDone >= 1 }),
    ONE_OF_EACH("uma-de-cada", "Um de cada", "Jogar cada modo pelo menos uma vez", { AchievementStats.MODES.all { m -> m in it.modesUsed } }),
    WORDS_1000("mil-palavras", "Mil palavras", "1 000 palavras resolvidas", { it.wordsSolved >= 1000 }),
    WORDS_100("cem-palavras", "Cem palavras", "100 palavras resolvidas", { it.wordsSolved >= 100 }),
    ALL_THEMES("temas-todos", "Todas as cores", "Jogar com cada tema visual", { it.visualThemesUsed.size >= 5 }),
    TWO_THEMES("tema-dois", "Mudança de ares", "Jogar com dois temas visuais", { it.visualThemesUsed.size >= 2 }),
    GRID_13("grelha-13", "Grande grelha", "Concluir uma grelha livre de 13×13", { 13 in it.freeGridSizesDone }),
    GRID_9("grelha-9", "Grelha média", "Concluir uma grelha livre de 9×9", { 9 in it.freeGridSizesDone }),
    FIRST_TIME_ATTACK("primeiro-contra", "Ao relógio", "Terminar um contra-relógio", { it.timeAttackBest != null }),
    STARTER("sem-conquistas-ainda", "Começo", "Ter pelo menos uma conquista", { false }),
    ;

    companion object {
        /** Ids of the unlocked achievements, computed from the player's numbers. */
        fun unlocked(stats: AchievementStats): Set<Achievement> = entries.filter { it.rule(stats) }.toSet()
    }
}

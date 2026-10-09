package app.palavrascruzadas.core.daily

import java.time.LocalDate

/**
 * Daily challenge seed: YYYYMMDD as a number. Copied from
 * `DailySeed.kt` in Sopa de Letras, so the same day gives the same puzzle on the phone and
 * in the pre-generated levels.
 */
fun dailySeedNumber(date: LocalDate): Long =
    date.year * 10000L + date.monthValue * 100L + date.dayOfMonth

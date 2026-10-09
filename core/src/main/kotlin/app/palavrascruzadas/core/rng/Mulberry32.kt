package app.palavrascruzadas.core.rng

/**
 * Mulberry32 pseudo-random generator: the same seed gives the same sequence on any machine.
 * Same algorithm as Sopa de Letras (32-bit arithmetic with overflow, like Math.imul).
 */
class Mulberry32(seed: Int) {
    private var state: Int = seed

    /** Next value in [0, 1). */
    fun nextDouble(): Double {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (1 or t)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }

    /** Integer in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive" }
        return (nextDouble() * bound).toInt()
    }
}

package app.palavrascruzadas.core.rng

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mulberry32Test {

    @Test
    fun `same seed gives the same sequence`() {
        val a = Mulberry32(12345)
        val b = Mulberry32(12345)
        repeat(1000) { assertEquals(a.nextDouble(), b.nextDouble(), 0.0) }
    }

    @Test
    fun `different seeds give different sequences`() {
        val a = Mulberry32(1)
        val b = Mulberry32(2)
        assertNotEquals(a.nextDouble(), b.nextDouble(), 0.0)
    }

    @Test
    fun `values stay in the unit interval`() {
        val r = Mulberry32(42)
        repeat(10_000) {
            val v = r.nextDouble()
            assertTrue("value $v out of [0,1)", v >= 0.0 && v < 1.0)
        }
    }

    @Test
    fun `nextInt stays within bound and covers it`() {
        val r = Mulberry32(7)
        val seen = BooleanArray(5)
        repeat(2_000) {
            val v = r.nextInt(5)
            assertTrue(v in 0..4)
            seen[v] = true
        }
        assertTrue(seen.all { it })
    }
}

package app.palavrascruzadas.core.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HintWalletTest {

    @Test
    fun `starts with ten hints`() {
        assertEquals(10, HintWallet().balance)
    }

    @Test
    fun `spending reduces the balance and fails without enough hints`() {
        val w = HintWallet(balance = 2)
        assertNull(w.spend(HintCost.REVEAL_WORD))
        assertEquals(1, w.spend(HintCost.REVEAL_LETTER)!!.balance)
    }

    @Test
    fun `verify word is free`() {
        assertEquals(0, HintCost.VERIFY_WORD.amount)
        assertEquals(HintWallet(balance = 0), HintWallet(balance = 0).spend(HintCost.VERIFY_WORD))
    }

    @Test
    fun `earning is capped at ninety-nine`() {
        val w = HintWallet(balance = 98).earn(HintReward.BOSS_WORLD)
        assertEquals(99, w.balance)
    }
}

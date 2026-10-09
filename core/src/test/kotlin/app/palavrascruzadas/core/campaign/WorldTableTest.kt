package app.palavrascruzadas.core.campaign

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldTableTest {
    @Test
    fun fifteenWorldsWithDistinctNumbers() {
        assertEquals((1..15).toList(), WorldTable.worlds.map { it.number })
    }

    @Test
    fun typesFollowSection82() {
        assertEquals(LevelType.WHEEL, WorldTable.typeAt(5))
        assertEquals(LevelType.WHEEL, WorldTable.typeAt(15))
        assertEquals(LevelType.WHEEL, WorldTable.typeAt(35))
        assertEquals(LevelType.KEYWORD, WorldTable.typeAt(10))
        assertEquals(LevelType.KEYWORD, WorldTable.typeAt(30))
        assertEquals(LevelType.DIRETAS, WorldTable.typeAt(20))
        assertEquals(LevelType.FIT_IT, WorldTable.typeAt(25))
        assertEquals(LevelType.BOSS, WorldTable.typeAt(40))
        assertEquals(LevelType.CLASSIC, WorldTable.typeAt(1))
    }

    @Test
    fun sizesAndWordCountsStayInsideTheTable() {
        for (world in WorldTable.worlds) {
            for (position in 1..WorldTable.LEVELS_PER_WORLD) {
                val boss = position == WorldTable.LEVELS_PER_WORLD
                val size = WorldTable.sizeAt(world, position)
                val words = WorldTable.wordsAt(world, position)
                val maxSize = if (boss) world.sizeTo + 1 else world.sizeTo
                val minSize = world.sizeFrom
                assert(size in minSize..maxSize) { "world ${world.number} pos $position size $size" }
                val maxWords = if (boss) world.wordsTo + 3 else world.wordsTo
                assert(words in world.wordsFrom..maxWords) { "world ${world.number} pos $position words $words" }
            }
        }
    }

    @Test
    fun firstWorldStartsSmall() {
        val first = WorldTable.worlds.first()
        assertEquals(5, WorldTable.sizeAt(first, 1))
        assertEquals(4, WorldTable.wordsAt(first, 1))
    }
}

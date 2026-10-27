package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MunnyHomeLayoutTest {
    @Test fun savedOrderSurvivesUnknownDuplicateAndNewModuleKeys() {
        val normalized=HomeModules.normalize(listOf("goals","obsolete","balance","goals"))
        assertEquals(listOf("goals","balance"),normalized.take(2))
        assertEquals(HomeModules.ids.toSet(),normalized.toSet())
        assertEquals(HomeModules.ids.size,normalized.size)
    }
    @Test fun draggingAcrossBothDirectionsKeepsEachModuleExactlyOnce() {
        val down=HomeModules.move(HomeModules.ids,"balance","goals")
        assertTrue(down.indexOf("balance")>down.indexOf("goals"))
        val up=HomeModules.move(down,"balance",down.first())
        assertEquals("balance",up.first())
        assertEquals(HomeModules.ids.toSet(),up.toSet())
        assertEquals(HomeModules.ids.size,up.size)
    }
}

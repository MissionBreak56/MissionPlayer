package com.missionbreak56.musicplayer

import com.missionbreak56.musicplayer.data.commonSpelling
import com.missionbreak56.musicplayer.data.sameText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LibraryGroupingTest {
    @Test fun artistNamesThatDifferOnlyInCaseOrSpacesAreTheSame() {
        assertEquals(sameText("Kamaitachi"), sameText("kamaitachi"))
        assertEquals(sameText("Kamaitachi"), sameText("  Kamaitachi  "))
        assertEquals(sameText("Homem  Torto"), sameText("homem torto"))
    }

    @Test fun differentNamesStayDifferent() {
        assertNotEquals(sameText("Kamaitachi"), sameText("Kamaitachi Band"))
        assertNotEquals(sameText("Homem Torto"), sameText("Homem Tortos"))
    }

    @Test fun theMostUsedSpellingIsShown() {
        assertEquals("Kamaitachi", commonSpelling(listOf("kamaitachi", "Kamaitachi", "Kamaitachi")))
        assertEquals("Kamaitachi", commonSpelling(listOf("Kamaitachi", "kamaitachi")))
    }
}

package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import org.junit.Assert.assertEquals
import org.junit.Test

class PromoBarLociTest {
    @Test
    fun `every 6 repeats and never ends the list with an ad`() {
        assertEquals(listOf(6, 13), spotStripPositions(tiles = 14, after = 6, every = 6)) // 6 tiles, ad, 6 tiles, ad, 2 tiles
        assertEquals(listOf(6), spotStripPositions(tiles = 12, after = 6, every = 6))
    }

    @Test
    fun `once, at the top, or after a short list`() {
        assertEquals(listOf(2), spotStripPositions(tiles = 30, after = 2, every = 0))
        assertEquals(listOf(0), spotStripPositions(tiles = 5, after = 0, every = 0))
        assertEquals(listOf(4), spotStripPositions(tiles = 4, after = 6, every = 6))
    }
}

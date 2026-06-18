package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class LoanMathTest {

    @Test
    fun testStandardEmi() {
        val emi = LoanMath.calculateEmi(100000.0, 10.0, 12)
        assertEquals(8791.59, emi, 0.1) // Standard EMI formula check
    }

    @Test
    fun testGenerateSchedule_Simple() {
        val input = LoanInput(100000.0, 10.0, 12)
        val summary = LoanMath.generateSchedule(input, emptyList(), emptyList())
        assertEquals(12, summary.effectiveTenureMonths)
        assertEquals(5499.0, summary.totalInterest, 2.0)
    }

    @Test
    fun testPrepayment_ReducesTenure() {
        val input = LoanInput(100000.0, 10.0, 12, RepaymentStrategy.REDUCE_TENURE)
        val prepayments = listOf(Prepayment(month = 1, amount = 20000.0))
        val summary = LoanMath.generateSchedule(input, prepayments, emptyList())
        
        // Tenure should be less than 12
        assert(summary.effectiveTenureMonths < 12)
        assert(summary.savingsTenureMonths > 0)
        assert(summary.savingsInterest > 1000.0)
    }
}

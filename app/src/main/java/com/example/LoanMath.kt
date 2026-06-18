package com.example

import kotlin.math.pow
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoanInput(
    val principal: Double,
    val annualInterestRate: Double,
    val tenureMonths: Int,
    val strategy: RepaymentStrategy = RepaymentStrategy.REDUCE_TENURE,
    val extraMonthlyPayment: Double = 0.0,
    val startDateMs: Long = System.currentTimeMillis()
)

enum class RepaymentStrategy {
    REDUCE_EMI,
    REDUCE_TENURE
}

@JsonClass(generateAdapter = true)
data class Prepayment(
    val month: Int,
    val amount: Double,
    val strategy: RepaymentStrategy = RepaymentStrategy.REDUCE_TENURE
)

@JsonClass(generateAdapter = true)
data class RateChange(
    val month: Int,
    val newInterestRate: Double
)

data class AmortizationRow(
    val month: Int,
    val principalPaid: Double,
    val interestPaid: Double,
    val openBalance: Double,
    val closeBalance: Double,
    val emi: Double,
    val prepayment: Double = 0.0,
    val rate: Double
)

data class LoanSummary(
    val expectedEmi: Double,
    val totalInterest: Double,
    val totalPayment: Double,
    val effectiveTenureMonths: Int,
    val savingsInterest: Double,
    val savingsTenureMonths: Int,
    val schedule: List<AmortizationRow>
)

object LoanMath {

    fun calculateEmi(principal: Double, annualRate: Double, months: Int): Double {
        if (principal <= 0 || months <= 0) return 0.0
        if (annualRate <= 0.0) return principal / months
        val r = annualRate / 12 / 100
        val p = principal
        val n = months
        return p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
    }

    fun generateSchedule(
        input: LoanInput,
        prepayments: List<Prepayment>,
        rateChanges: List<RateChange>
    ): LoanSummary {
        if (input.principal <= 0 || input.tenureMonths <= 0) {
            return LoanSummary(0.0, 0.0, 0.0, 0, 0.0, 0, emptyList())
        }

        val prepayMap = prepayments.groupBy { it.month }
        val rateMap = rateChanges.associateBy { it.month }

        var currentBalance = input.principal
        var currentRate = input.annualInterestRate
        var currentEmi = calculateEmi(currentBalance, currentRate, input.tenureMonths)
        var remainingMonths = input.tenureMonths

        val schedule = mutableListOf<AmortizationRow>()
        var totalInterest = 0.0
        var totalPayment = 0.0
        var month = 1

        while (currentBalance > 0.005) {
            // Check rate change
            if (rateMap.containsKey(month)) {
                currentRate = rateMap[month]!!.newInterestRate
                if (input.strategy == RepaymentStrategy.REDUCE_EMI) {
                    currentEmi = calculateEmi(currentBalance, currentRate, remainingMonths)
                } else {
                    val monthlyInterestRate = currentRate / 12 / 100
                    if (currentEmi <= currentBalance * monthlyInterestRate) {
                         currentEmi = calculateEmi(currentBalance, currentRate, remainingMonths)
                    }
                }
            }

            val monthlyInterest = currentBalance * (currentRate / 12 / 100)
            
            // Check prepayment
            var prepay = input.extraMonthlyPayment
            var shouldReduceEmi = (input.extraMonthlyPayment > 0 && input.strategy == RepaymentStrategy.REDUCE_EMI)
            
            if (prepayMap.containsKey(month)) {
                for (p in prepayMap[month]!!) {
                    prepay += p.amount
                    if (p.strategy == RepaymentStrategy.REDUCE_EMI) {
                        shouldReduceEmi = true
                    }
                }
            }

            // Normal payment
            var principalPaid = currentEmi - monthlyInterest
            if (currentBalance + monthlyInterest <= currentEmi) {
                principalPaid = currentBalance
                currentEmi = currentBalance + monthlyInterest
            }

            if (prepay > currentBalance - principalPaid) {
                prepay = currentBalance - principalPaid
            }

            val openBalance = currentBalance
            var closeBalance = currentBalance - principalPaid - prepay
            if (closeBalance < 0.005) {
                closeBalance = 0.0
            }

            schedule.add(
                AmortizationRow(
                    month = month,
                    principalPaid = principalPaid,
                    interestPaid = monthlyInterest,
                    openBalance = openBalance,
                    closeBalance = closeBalance,
                    emi = currentEmi,
                    prepayment = prepay,
                    rate = currentRate
                )
            )

            totalInterest += monthlyInterest
            totalPayment += (principalPaid + monthlyInterest + prepay)
            currentBalance = closeBalance

            if (prepay > 0 && shouldReduceEmi) {
                if (remainingMonths > 1) {
                    currentEmi = calculateEmi(currentBalance, currentRate, remainingMonths - 1)
                }
            }

            remainingMonths--
            month++
            if (month > input.tenureMonths * 2) break // Safety exit
        }

        val baseEmi = calculateEmi(input.principal, input.annualInterestRate, input.tenureMonths)
        val baseTotalPayment = baseEmi * input.tenureMonths
        val baseTotalInterest = baseTotalPayment - input.principal
        
        val savingsInterest = baseTotalInterest - totalInterest
        val savingsTenure = input.tenureMonths - (month - 1)

        return LoanSummary(
            expectedEmi = baseEmi,
            totalInterest = totalInterest,
            totalPayment = totalPayment,
            effectiveTenureMonths = month - 1,
            savingsInterest = if (savingsInterest > 0) savingsInterest else 0.0,
            savingsTenureMonths = if (savingsTenure > 0) savingsTenure else 0,
            schedule = schedule
        )
    }
}

package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MunnyPlanningMathTest {
    @Test fun simulatorWithZeroInterestAddsPrincipalAndMonthlyContributions() {
        val result=MunnyInvestmentMath.calculate(10_000L,2_500L,0.0,"monthly",12)
        assertEquals(40_000L,result.finalCents)
        assertEquals(40_000L,result.investedCents)
        assertEquals(0L,result.interestCents)
        assertEquals(12,result.months)
    }

    @Test fun monthlyCompoundInterestAddsContributionsAfterInterest() {
        // 10000 -> 10100 + 500 = 10600 -> 10706 + 500 = 11206
        val result=MunnyInvestmentMath.calculate(10_000L,500L,1.0,"monthly",2)
        assertEquals(11_206L,result.finalCents)
        assertEquals(11_000L,result.investedCents)
        assertEquals(206L,result.interestCents)
        assertEquals(1.0/100.0,result.effectiveMonthlyRate,0.0000001)
    }

    @Test fun annualEffectiveRateConvertsToMonthlyEquivalent() {
        val result=MunnyInvestmentMath.calculate(10_000L,0L,12.0,"annual",12)
        assertEquals(11_200L,result.finalCents)
        assertEquals(1.12,result.points.last().amountCents.toDouble()/10_000,0.0001)
        assertEquals(Math.pow(1.12,1.0/12.0)-1.0,result.effectiveMonthlyRate,0.00000001)
    }

    @Test fun invalidInvestmentInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            MunnyInvestmentMath.calculate(-1L,0L,10.0,"annual",12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MunnyInvestmentMath.calculate(10_000L,0L,-0.1,"annual",12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MunnyInvestmentMath.calculate(10_000L,0L,5.0,"annual",601)
        }
    }

    @Test fun healthDoesNotCountCardPurchaseTwiceAsCashExpense() {
        val accounts=listOf(MoneyAccount("a","Principal",50_000L))
        val cards=listOf(MoneyCard("c","Crédito",500_000L,15,25))
        val movements=listOf(
            MoneyMovement("income","income",100_000L,"2026-10-02",
                accountId="a",status="paid"),
            MoneyMovement("expense","expense",30_000L,"2026-10-03",
                accountId="a",status="paid"),
            MoneyMovement("card","card",20_000L,"2026-10-04",
                cardId="c",invoiceMonth="2026-10",status="planned")
        )
        val summary=MunnyPlanningMath.health(accounts,cards,movements,emptySet(),
            today=LocalDate.parse("2026-10-22"))
        assertEquals(120_000L,summary.bankBalance)
        assertEquals(20_000L,summary.openCardBills)
        assertEquals(100_000L,summary.netPosition)
        assertEquals(50_000L,summary.consumption)
        assertEquals(100_000L,summary.income)
        assertEquals(50_000L,summary.margin)
        assertEquals(50.0,summary.commitmentPercent!!,0.0001)
        assertTrue(summary.alerts.any{it.title=="Fatura próxima"})
    }

    @Test fun historicalMonthlyContributionNeedsTwoMonthsOfIncome() {
        val accounts=listOf(MoneyAccount("a","Principal",0L))
        val movements=listOf(
            MoneyMovement("augIn","income",90_000L,"2026-08-01",accountId="a",status="paid"),
            MoneyMovement("augOut","expense",50_000L,"2026-08-02",accountId="a",status="paid"),
            MoneyMovement("sepIn","income",120_000L,"2026-09-01",accountId="a",status="paid"),
            MoneyMovement("sepOut","expense",40_000L,"2026-09-02",accountId="a",status="paid")
        )
        val summary=MunnyPlanningMath.health(accounts,emptyList(),movements,emptySet(),
            today=LocalDate.parse("2026-10-22"))
        assertEquals(20_000L,summary.illustrativeContribution)
        val oneMonth=MunnyPlanningMath.health(accounts,emptyList(),movements.take(2),
            emptySet(),today=LocalDate.parse("2026-10-22"))
        assertNull(oneMonth.illustrativeContribution)
    }

    @Test fun noIncomeMeansCommitmentRatioIsUnavailable() {
        val summary=MunnyPlanningMath.health(emptyList(),emptyList(),emptyList(),emptySet(),
            today=LocalDate.parse("2026-10-22"))
        assertNull(summary.commitmentPercent)
        assertNull(summary.illustrativeContribution)
        assertEquals(6,summary.trend.size)
        assertTrue(summary.alerts.isEmpty())
    }
}

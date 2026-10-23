package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MunnyCardsGoalsTest {
    @Test fun usedLimitCountsUnpaidPurchasesAndFutureInstallments() {
        val card=MoneyCard("cardA","Pessoal",100_000L,15,25)
        val entries=listOf(
            MoneyMovement("part1","card",35_000L,"2026-10-10",cardId="cardA",
                invoiceMonth="2026-10"),
            MoneyMovement("part2","card",25_000L,"2026-11-10",cardId="cardA",
                invoiceMonth="2026-11"),
            MoneyMovement("otherCard","card",90_000L,"2026-10-09",cardId="other",
                invoiceMonth="2026-10"),
            MoneyMovement("billPaid","cardPayment",10_000L,"2026-10-16",
                status="paid",cardId="cardA",invoiceMonth="2026-10",accountId="bank")
        )
        val position=MunnyCardMath.position(card,entries,emptySet(),LocalDate.parse("2026-10-23"))
        assertEquals(50_000L,position.committed)
        assertEquals(50_000L,position.available)
        assertEquals(0L,position.overLimit)
        assertEquals(2,position.bills.size)
        assertEquals(3,position.movements.size)
        assertEquals(.5f,position.utilization,0.00001f)
    }
    @Test fun cardOverLimitIsNotAFalseNegativeAvailableValue() {
        val card=MoneyCard("card","Limite",10_000L,15,25)
        val movements=listOf(MoneyMovement("x","card",15_000L,"2026-10-10",
            cardId="card",invoiceMonth="2026-10"))
        val pos=MunnyCardMath.position(card,movements,emptySet(),LocalDate.parse("2026-10-23"))
        assertEquals(15_000L,pos.committed)
        assertEquals(0L,pos.available)
        assertEquals(5_000L,pos.overLimit)
        assertEquals(1f,pos.utilization,0f)
    }
    @Test fun goalExampleThreeThousandToFiftyThousandAtOneThousandMonthly() {
        val plan=MunnyGoalMath.calculate(5_000_000L,300_000L,100_000L,
            "2030-12-31",LocalDate.parse("2026-10-23"))
        assertEquals(4_700_000L,plan.missing)
        assertEquals(47,plan.monthsToGoal)
        assertEquals("2030-09",plan.projectedDate.toString())
        assertEquals(.06f,plan.progress,0.000001f)
    }
    @Test fun deadlineContributionRoundsUpAndNoContributionIsUnavailable() {
        val date=LocalDate.parse("2026-10-23")
        val plan=MunnyGoalMath.calculate(100_000L,10_000L,0L,"2027-04-10",date)
        assertEquals(6,plan.monthsUntilDeadline)
        assertEquals(15_000L,plan.requiredPerMonthForDeadline)
        assertNull(plan.monthsToGoal)
        val shorter=MunnyGoalMath.calculate(100_000L,10_000L,25_000L,"2027-04-10",date)
        assertEquals(4,shorter.monthsToGoal)
    }
    @Test fun completedGoalIsImmediateAndPastDeadlineHasNoRecommendedPayment() {
        val today=LocalDate.parse("2026-10-23")
        val complete=MunnyGoalMath.calculate(50_000L,55_000L,0L,"2027-01-01",today)
        assertEquals(0,complete.monthsToGoal)
        assertEquals(0L,complete.requiredPerMonthForDeadline)
        assertEquals(0L,complete.missing)
        val missed=MunnyGoalMath.calculate(50_000L,0L,1_000L,"2026-01-01",today)
        assertNull(missed.requiredPerMonthForDeadline)
        assertTrue(missed.deadlineReached)
    }
}

package app.munny.nativeapp

import java.time.LocalDate
import java.time.YearMonth

/**
 * Goal planning assumes no investment return (0% interest) and an end-of-month
 * contribution. This deliberately separates mathematical possibilities from
 * recorded movements or recommendations to transfer funds.
 */
data class MunnyGoalScenario(
    val target:Long,
    val saved:Long,
    val missing:Long,
    val monthlyContribution:Long,
    val progress:Float,
    val monthsToGoal:Int?,
    val projectedDate:YearMonth?,
    val monthsUntilDeadline:Int,
    val requiredPerMonthForDeadline:Long?,
    val deadlineReached:Boolean
)
object MunnyGoalMath {
    fun calculate(
        target:Long,saved:Long,monthlyContribution:Long,
        deadline:String, today:LocalDate=LocalDate.now()
    ):MunnyGoalScenario {
        require(target>0L&&saved>=0L&&monthlyContribution>=0L)
        val missing=(target-saved).coerceAtLeast(0L)
        val expiry=LocalDate.parse(deadline)
        val months=((expiry.year-today.year)*12+(expiry.monthValue-today.monthValue))
            .coerceAtLeast(0)
        // Rounds up to the next contribution whenever the remaining period
        // is less than one whole month.
        val time=if(expiry.isAfter(today))months.coerceAtLeast(1) else 0
        val monthsNeeded=when {
            missing==0L->0
            monthlyContribution==0L->null
            else->{
                val whole=missing/monthlyContribution
                val rounded=whole+if(missing%monthlyContribution>0L)1L else 0L
                rounded.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            }
        }
        val deadlineNeed=when {
            missing==0L->0L
            time<=0->null
            else->missing/time+if(missing%time>0)1L else 0L
        }
        return MunnyGoalScenario(
            target,saved,missing,monthlyContribution,
            (saved.toDouble()/target.toDouble()).toFloat().coerceIn(0f,1f),
            monthsNeeded,monthsNeeded?.let{YearMonth.from(today).plusMonths(it.toLong())},
            time,deadlineNeed,expiry.isBefore(today)
        )
    }
}

package app.munny.nativeapp

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.pow
import kotlin.math.roundToLong

/** Financial position is NOT full net worth: investments, loans and other
 *  assets/liabilities outside the Munny ledger are not included. */
data class MunnyMonthPosition(
    val month:YearMonth,val bankBalance:Long,val openCardBills:Long,
    val netPosition:Long,val income:Long,val consumption:Long
)
data class MunnyHealthAlert(
    val title:String,val details:String,val date:String,
    val priority:Int,val amount:Long?=null
)
data class MunnyHealthSummary(
    val month:YearMonth,val bankBalance:Long,val openCardBills:Long,
    val netPosition:Long,val income:Long,val consumption:Long,
    val commitmentPercent:Double?,val margin:Long,
    val illustrativeContribution:Long?,
    val trend:List<MunnyMonthPosition>,val alerts:List<MunnyHealthAlert>
)
data class BudgetIndicator(val category:String,val month:String,val limit:Long)

object MunnyPlanningMath {
    fun position(
        accounts:List<MoneyAccount>,cards:List<MoneyCard>,
        movements:List<MoneyMovement>,exceptions:Set<String>,
        at:LocalDate
    ):MunnyMonthPosition {
        val month=YearMonth.from(at)
        val balance=FinanceMath.breakdown(accounts,movements,exceptions,at.toString()).total
        val open=FinanceMath.invoices(cards,movements,exceptions,at.toString())
            .sumOf{it.remaining}
        val flow=FinanceMath.month(movements,exceptions,month.toString())
        return MunnyMonthPosition(month,balance,open,balance-open,flow.income,flow.consumption)
    }
    fun health(
        accounts:List<MoneyAccount>,cards:List<MoneyCard>,
        movements:List<MoneyMovement>,exceptions:Set<String>,
        budgets:List<BudgetIndicator> = emptyList(),
        today:LocalDate=LocalDate.now()
    ):MunnyHealthSummary {
        val month=YearMonth.from(today)
        val now=position(accounts,cards,movements,exceptions,today)
        val commitment=if(now.income>0L)
            now.consumption.toDouble()*100.0/now.income else null
        val historical=(5 downTo 0).map{back->
            val m=month.minusMonths(back.toLong())
            position(accounts,cards,movements,exceptions,
                if(back==0)today else m.atEndOfMonth())
        }
        // A cautious input for the simulator, not personalized investment advice.
        // Requires >=2 months with income in last 3 completed months.
        val funded=(1..3).map{month.minusMonths(it.toLong())}
            .map{FinanceMath.month(movements,exceptions,it.toString())}
            .filter{it.income>0L}
        val scenarioContribution=if(funded.size>=2)
            funded.minOf{it.income-it.consumption}.coerceAtLeast(0L)/2 else null

        val cutoff=today.plusDays(30)
        val warnings=mutableListOf<MunnyHealthAlert>()
        val cardNames=cards.associateBy{it.id}
        FinanceMath.invoices(cards,movements,exceptions,today.plusYears(10).toString())
            .filter{it.remaining>0 && it.due<=cutoff.toString()}.take(12)
            .forEach{bill->
                warnings+=MunnyHealthAlert(
                    if(bill.due<today.toString())"Fatura vencida" else "Fatura próxima",
                    "${cardNames[bill.cardId]?.name?:"Cartão"} · ${bill.due}",
                    bill.due,if(bill.due<today.toString())0 else 1,bill.remaining)
            }
        FinanceMath.occurrences(movements,exceptions,cutoff.toString())
            .filter{it.kind=="expense"&&it.status=="planned"&&it.date<=cutoff.toString()}
            .take(12).forEach{movement->
                warnings+=MunnyHealthAlert(
                    if(movement.date<today.toString())"Despesa prevista atrasada" else "Despesa prevista",
                    movement.title,movement.date,
                    if(movement.date<today.toString())0 else 2,movement.amount)
            }
        val thisMonthRecords=FinanceMath.occurrences(movements,exceptions,month.atEndOfMonth().toString())
            .filter{it.date.startsWith(month.toString())}
        budgets.filter{it.month==month.toString()&&it.limit>0L}
            .forEach {budget->
                val used=thisMonthRecords.filter {
                    it.category==budget.category &&
                        (it.kind=="card"||(it.kind=="expense"&&it.status=="paid"))
                }.sumOf{it.amount}
                if(used>=budget.limit)warnings+=MunnyHealthAlert(
                    "Limite ultrapassado",budget.category,today.toString(),0,used-budget.limit)
                else if(used.toDouble()>=budget.limit.toDouble()*.8)warnings+=MunnyHealthAlert(
                    "Limite perto de 80%",budget.category,today.toString(),2,used)
            }
        if(commitment!=null && commitment>=100.0)warnings+=MunnyHealthAlert(
            "Consumo supera entradas confirmadas",
            "O consumo registrado supera a renda confirmada neste mês.",
            today.toString(),0)
        return MunnyHealthSummary(month,now.bankBalance,now.openCardBills,
            now.netPosition,now.income,now.consumption,commitment,
            now.income-now.consumption,scenarioContribution,historical,
            warnings.sortedWith(compareBy<MunnyHealthAlert>{it.priority}.thenBy{it.date}).take(8))
    }
}

data class MunnyInvestmentPoint(val month:Int,val amountCents:Long)
data class MunnyInvestmentProjection(
    val initialCents:Long,val monthlyContributionCents:Long,
    val months:Int,val effectiveMonthlyRate:Double,
    val investedCents:Long,val finalCents:Long,val interestCents:Long,
    val points:List<MunnyInvestmentPoint>
)
object MunnyInvestmentMath {
    /** End-of-month contributions, constant nominal effective return, no tax/fees. */
    fun calculate(
        initialCents:Long,monthlyContributionCents:Long,
        ratePercent:Double,ratePeriod:String,months:Int
    ):MunnyInvestmentProjection {
        require(initialCents>=0L&&monthlyContributionCents>=0L){"Informe valores não negativos."}
        require(initialCents<=100_000_000_000_000L&&
            monthlyContributionCents<=100_000_000_000_000L){"Montante de entrada elevado demais."}
        require(ratePercent.isFinite()&&ratePercent in 0.0..100.0) {
            "A taxa deve estar entre 0% e 100%."
        }
        require(ratePeriod=="monthly"||ratePeriod=="annual"){"Selecione taxa mensal ou anual."}
        require(months in 1..600){"O prazo deve estar entre 1 e 600 meses."}
        val rate=ratePercent/100.0
        val monthly=if(ratePeriod=="annual")(1.0+rate).pow(1.0/12.0)-1.0 else rate
        var balance=initialCents.toDouble()
        val points=mutableListOf(MunnyInvestmentPoint(0,initialCents))
        val step=when{months<=24->1;months<=120->3;else->12}
        for(i in 1..months) {
            balance=balance*(1.0+monthly)+monthlyContributionCents
            require(balance.isFinite()&&balance<=Long.MAX_VALUE.toDouble()/4) {
                "O montante final ultrapassou o limite de cálculo."
            }
            if(i%step==0||i==months)points+=MunnyInvestmentPoint(i,balance.roundToLong())
        }
        val invested=initialCents+monthlyContributionCents*months
        val final=balance.roundToLong()
        return MunnyInvestmentProjection(initialCents,monthlyContributionCents,
            months,monthly,invested,final,(final-invested).coerceAtLeast(0L),points)
    }
}

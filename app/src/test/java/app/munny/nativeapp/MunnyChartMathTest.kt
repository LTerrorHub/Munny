package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MunnyChartMathTest {
    private fun expense(id:String,date:String,amount:Long,category:String="Outros")=
        MoneyMovement(id,"expense",amount,date,status="paid",category=category)
    @Test fun chartVariationsKeepTheSameConsumptionAndGroupTailWithoutLosingAmounts() {
        val rows=(1..9).map{expense("$it","2026-10-02",it*100L,"Categoria $it")}+
            MoneyMovement("card","card",500,"2026-10-02",cardId="c",category="Cartão")+
            MoneyMovement("payment","cardPayment",800,"2026-10-02",status="paid")+
            MoneyMovement("transfer","transfer",900,"2026-10-02",status="paid")+
            MoneyMovement("planned","expense",500,"2026-10-02",status="planned")
        val total=MunnyTransactionQuery.consumption(rows)
        val detailed=MunnyChartMath.shares(rows,"categories")
        val compact=MunnyChartMath.shares(rows,"categories",5)
        assertEquals(total,detailed.sumOf{it.amount})
        assertEquals(total,compact.sumOf{it.amount})
        assertEquals(5,compact.size)
        assertEquals("__other__",compact.last().key)
        assertEquals(total,MunnyChartMath.shares(rows,"channels").sumOf{it.amount})
    }
    @Test fun dailyTrendInsertsZeroesAndDoesNotTreatTransferOrPaymentAsSpending() {
        val rows=listOf(expense("a","2026-10-01",100),expense("b","2026-10-03",300),
            MoneyMovement("p","cardPayment",9_000,"2026-10-02",status="paid"))
        val trend=MunnyChartMath.trend(rows)
        assertTrue(trend.daily)
        assertEquals(listOf(100L,0L,300L),trend.points.map{it.consumption})
        assertEquals(400L,trend.points.sumOf{it.consumption})
    }
    @Test fun monthlyTrendFillsMissingMonthsAndLabelsTrimmedRange() {
        val rows=listOf(expense("a","2025-01-01",100),expense("b","2026-03-01",300))
        val trend=MunnyChartMath.trend(rows,12)
        assertFalse(trend.daily)
        assertEquals(12,trend.points.size)
        assertEquals(3,trend.omitted)
        assertEquals("2025-04",trend.points.first().date)
        assertEquals(0L,trend.points.first().consumption)
        assertEquals(300L,trend.points.last().consumption)
    }
    @Test fun onePointEmptySeriesAndIncompatibleStylesAreHandled() {
        assertTrue(MunnyChartMath.trend(emptyList()).points.isEmpty())
        assertEquals(1,MunnyChartMath.trend(listOf(expense("one","2026-10-27",100))).points.size)
        assertEquals("line",MunnyChartMath.safeStyle("trend","donut"))
        assertEquals("donut",MunnyChartMath.safeStyle("categories","line"))
        assertEquals("bars",MunnyChartMath.safeStyle("channels","bars"))
        assertEquals("categories",MunnyChartMath.safeTopic("invalid"))
    }
}

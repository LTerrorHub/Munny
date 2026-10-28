package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MunnyTransactionQueryTest {
    private val rows=listOf(
        MoneyMovement("income","income",100_000,"2026-10-02",accountId="a",status="paid",title="Salário"),
        MoneyMovement("expense","expense",20_000,"2026-10-03",accountId="a",status="paid",category="Alimentação",title="Mercado"),
        MoneyMovement("planned","expense",9_000,"2026-10-04",accountId="a",status="planned",category="Alimentação"),
        MoneyMovement("card","card",10_000,"2026-10-05",cardId="c",invoiceMonth="2026-10",category="Alimentação"),
        MoneyMovement("payment","cardPayment",10_000,"2026-10-06",accountId="a",cardId="c",status="paid"),
        MoneyMovement("transfer","transfer",4_000,"2026-10-07",accountId="a",toAccountId="b",status="paid"),
        MoneyMovement("older","expense",7_000,"2026-09-07",accountId="b",status="paid"))
    @Test fun graphsAndSummaryUseExactlyFilteredRowsWithoutDoubleCounting() {
        val selected=MunnyTransactionQuery.select(rows,emptySet(),TransactionFilter(),"2026-10")
        assertEquals(6,selected.size)
        assertEquals(30_000L,MunnyTransactionQuery.consumption(selected))
        assertEquals(100_000L,MunnyTransactionQuery.income(selected))
        assertEquals(listOf("Alimentação" to 30_000L),MunnyTransactionQuery.categories(selected))
        assertEquals(setOf("2026-10"),MunnyTransactionQuery.months(selected).keys)
    }
    @Test fun destinationAccountMatchesTransferAndCardOriginIncludesPayments() {
        val transfer=MunnyTransactionQuery.select(rows,emptySet(),TransactionFilter(source="account:b"),"2026-10")
        assertEquals(listOf("transfer"),transfer.map{it.id})
        val card=MunnyTransactionQuery.select(rows,emptySet(),TransactionFilter(source="card:c"),"2026-10")
        assertEquals(setOf("card","payment"),card.map{it.id}.toSet())
        assertEquals(10_000L,MunnyTransactionQuery.consumption(card))
    }
    @Test fun rangeIsInclusiveAndSearchStatusAndExclusionsApplyTogether() {
        val filter=TransactionFilter(kind="expense",status="paid",period="custom",from="2026-10-03",to="2026-10-03")
        assertEquals(listOf("expense"),MunnyTransactionQuery.select(rows,emptySet(),filter,"2026-11","Mercado").map{it.id})
        assertTrue(MunnyTransactionQuery.select(rows,setOf("expense"),filter,"2026-10").isEmpty())
        assertTrue(MunnyTransactionQuery.select(rows,emptySet(),filter,"2026-10","inexistente").isEmpty())
    }
    @Test(expected=IllegalArgumentException::class) fun invertedRangeIsRejected() {
        TransactionFilter(period="custom",from="2026-10-10",to="2026-09-10").validate()
    }
    @Test fun allHistoryAndOldestOrderingRemainConsistent() {
        val all=MunnyTransactionQuery.select(rows,emptySet(),TransactionFilter(period="all"),"2026-10",oldestFirst=true)
        assertEquals("older",all.first().id)
        assertEquals(37_000L,MunnyTransactionQuery.consumption(all))
        assertEquals(37_000L,MunnyTransactionQuery.categories(all).sumOf{it.second})
    }
}

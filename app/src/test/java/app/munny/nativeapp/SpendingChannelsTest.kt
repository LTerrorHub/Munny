package app.munny.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingChannelsTest {
    @Test fun cardExpenseTransferAndInvoicePaymentAreSeparated() {
        val records=listOf(
            MoneyMovement("card","card",8000L,"2026-10-02",cardId="c",invoiceMonth="2026-10",title="Compra"),
            MoneyMovement("expense","expense",2000L,"2026-10-03",accountId="a",status="paid",title="Despesa"),
            MoneyMovement("transfer","transfer",5000L,"2026-10-04",accountId="a",toAccountId="b",status="paid",title="Movimento"),
            MoneyMovement("invoice","cardPayment",8000L,"2026-10-05",accountId="a",status="paid",title="Fatura"),
            MoneyMovement("income","income",7000L,"2026-10-06",accountId="a",status="paid",title="Entrada"),
            MoneyMovement("planned","expense",1000L,"2026-10-07",accountId="a",status="planned",title="Prevista")
        )
        val v=FinanceMath.spendingChannels(records,emptySet(),"2026-10")
        assertEquals(8000L,v.card)
        assertEquals(2000L,v.direct)
        assertEquals(10000L,v.consumption)
        assertEquals(5000L,v.transferOut)
        assertEquals(5000L,v.transferIn)
        assertEquals(8000L,v.invoicePayments)
        assertEquals(7000L,v.income)
        assertEquals(.8f,v.cardPercentage,.0001f)
        assertEquals(.2f,v.directPercentage,.0001f)
        assertEquals(10000L,FinanceMath.month(records,emptySet(),"2026-10").consumption)
    }

    @Test fun internalTransfersCancelInConsolidatedCash() {
        val accounts=listOf(MoneyAccount("a","Principal",10000L),MoneyAccount("b","Reserva",0L))
        val entries=listOf(MoneyMovement("t","transfer",3000L,"2026-10-02",
            accountId="a",toAccountId="b",status="paid",title="Guardar"))
        val all=FinanceMath.breakdown(accounts,entries,emptySet(),"2026-10-31")
        val first=FinanceMath.breakdown(accounts,entries,emptySet(),"2026-10-31","a")
        val second=FinanceMath.breakdown(accounts,entries,emptySet(),"2026-10-31","b")
        assertEquals(10000L,all.total)
        assertEquals(7000L,first.total)
        assertEquals(3000L,second.total)
    }

    @Test fun cardPurchaseDoesNotDebitCashUntilCardPayment() {
        val accounts=listOf(MoneyAccount("a","Principal",30000L))
        val buy=MoneyMovement("p","card",12000L,"2026-10-03",
            cardId="c",status="planned",invoiceMonth="2026-10",title="Compra")
        assertEquals(30000L,FinanceMath.breakdown(accounts,listOf(buy),emptySet(),"2026-10-31").total)
        val paid=MoneyMovement("pg","cardPayment",12000L,"2026-10-08",
            accountId="a",status="paid",cardId="c",invoiceMonth="2026-10",title="Pagamento")
        assertEquals(18000L,FinanceMath.breakdown(accounts,listOf(buy,paid),emptySet(),"2026-10-31").total)
        assertEquals(12000L,FinanceMath.month(listOf(buy,paid),emptySet(),"2026-10").consumption)
    }

    @Test fun zeroConsumptionHasZeroPercentages() {
        val v=FinanceMath.spendingChannels(emptyList(),emptySet(),"2026-10")
        assertEquals(0f,v.cardPercentage,.0001f)
        assertEquals(0f,v.directPercentage,.0001f)
        assertTrue(v.consumption==0L)
    }
}

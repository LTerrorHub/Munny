package app.munny.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

/** Two incomes enter different wallets, but the family total consolidates them.
 * Internal transfers between registered wallets must never inflate that total. */
class MunnyWalletAggregationTest {
    @Test fun salariesInSeparateWalletsAddToFamilyBalance() {
        val wallets=listOf(
            MoneyAccount("a","Conta pessoal",0L),
            MoneyAccount("b","Conta esposa",0L)
        )
        val movements=listOf(
            MoneyMovement("sa","income",500000L,"2026-10-02",
                accountId="a",status="paid"),
            MoneyMovement("sb","income",400000L,"2026-10-02",
                accountId="b",status="paid"),
            MoneyMovement("da","expense",150000L,"2026-10-03",
                accountId="a",status="paid"),
            MoneyMovement("db","expense",120000L,"2026-10-03",
                accountId="b",status="paid"),
            MoneyMovement("ab","transfer",100000L,"2026-10-04",
                accountId="a",toAccountId="b",status="paid")
        )
        val whenDate="2026-10-25"
        val family=FinanceMath.breakdown(wallets,movements,emptySet(),whenDate)
        val personal=FinanceMath.breakdown(wallets,movements,emptySet(),whenDate,"a")
        val spouse=FinanceMath.breakdown(wallets,movements,emptySet(),whenDate,"b")
        assertEquals(630000L,family.total)
        assertEquals(250000L,personal.total)
        assertEquals(380000L,spouse.total)
        assertEquals(family.total,personal.total+spouse.total)
    }
    @Test fun cardPurchaseAffectsConsumptionButNotBankBalanceUntilPayment() {
        val wallets=listOf(MoneyAccount("a","Banco",300000L))
        val card=MoneyMovement("buy","card",25000L,"2026-10-04",
            cardId="c",invoiceMonth="2026-10",status="planned")
        val balanceBefore=FinanceMath.breakdown(wallets,listOf(card),emptySet(),"2026-10-25")
        assertEquals(300000L,balanceBefore.total)
        assertEquals(25000L,FinanceMath.month(listOf(card),emptySet(),"2026-10").consumption)
        val paid=MoneyMovement("payment","cardPayment",25000L,"2026-10-06",
            cardId="c",invoiceMonth="2026-10",accountId="a",status="paid")
        assertEquals(275000L,FinanceMath.breakdown(wallets,listOf(card,paid),
            emptySet(),"2026-10-25").total)
        assertEquals(25000L,FinanceMath.month(listOf(card,paid),emptySet(),"2026-10").consumption)
    }
}

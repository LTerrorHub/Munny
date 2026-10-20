import app.munny.nativeapp.*
fun main() {
    val a = listOf(MoneyAccount("a", "Carteira", 100_000), MoneyAccount("b", "Conta", 0))
    val ts = listOf(
        MoneyMovement("1", "income", 50_000, "2026-10-03", "a", status="paid"),
        MoneyMovement("2", "expense", 20_000, "2026-10-05", "a", status="paid"),
        MoneyMovement("3", "transfer", 8_000, "2026-10-08", "a", "b", status="paid"),
        MoneyMovement("4", "card", 30_000, "2026-10-10", cardId="c", invoiceMonth="2026-10"),
        MoneyMovement("5", "cardPayment", 10_000, "2026-10-13", "a",status="paid",cardId="c",invoiceMonth="2026-10")
    )
    val cards = listOf(MoneyCard("c", "Visa", 200_000, 25, 5))
    val b = FinanceMath.breakdown(a,ts,emptySet(),"2026-10-19")
    check(b.total==120_000L) { "Esperado R$ 1.200: ${b.total}" }
    check(FinanceMath.breakdown(a,ts,emptySet(),"2026-10-19", "a").total==112_000L)
    check(FinanceMath.breakdown(a,ts,emptySet(),"2026-10-19", "b").total==8_000L)
    check(FinanceMath.month(ts,emptySet(),"2026-10").consumption==50_000L)
    check(FinanceMath.invoices(cards,ts,emptySet(),"2026-12-31").single().remaining==20_000L)
    val parts = FinanceMath.installments(MoneyMovement("x","card",1001,"2026-01-30"),3,"2026-02")
    check(parts.map{it.amount}==listOf(334L,334L,333L))
    val projected = FinanceMath.projected(a,cards,ts,emptySet(),"2026-11-05")
    check(projected==100_000L) { "Projetado R$1000 após saldo da fatura: $projected" }
    println("OK: saldo, transferência, consumo, faturas e parcelamento")
}

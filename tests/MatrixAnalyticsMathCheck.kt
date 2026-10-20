import app.munny.nativeapp.*
import java.time.YearMonth
fun main() {
    val records = listOf(
        MoneyMovement("1","income",140000,"2026-10-01",status="paid"),
        MoneyMovement("2","expense",9000,"2026-10-04",status="paid",category="Alimentação"),
        MoneyMovement("3","expense",5000,"2026-10-06",status="planned",category="Alimentação"),
        MoneyMovement("4","card",20000,"2026-10-07",category="Compras"),
        MoneyMovement("5","cardPayment",8000,"2026-10-11",status="paid"),
        MoneyMovement("6","expense",2500,"2026-09-03",status="paid",category="Transporte")
    )
    val m=FinanceMath.month(records,emptySet(),YearMonth.of(2026,10).toString())
    check(m.income == 140000L && m.consumption == 29000L)
    val included=FinanceMath.occurrences(records,emptySet(),"2026-10-31")
        .filter{it.date.startsWith("2026-10") && (it.kind=="card"||(it.kind=="expense" && it.status=="paid"))}
        .groupBy{it.category}.mapValues{(_,list)->list.sumOf{it.amount}}
    check(included.values.sum()==m.consumption)
    check(included["Alimentação"]==9000L && included["Compras"]==20000L)
    println("OK: as categorias reconciliam com o consumo e não contam despesas previstas ou pagamento da fatura duas vezes")
}

package app.munny.nativeapp

import java.time.LocalDate
import java.time.YearMonth

data class TransactionFilter(
    val kind:String="all",val source:String="all",val status:String="all",
    val period:String="month",val from:String="",val to:String=""
) {
    fun validate() {
        if(period=="custom") {
            val start=LocalDate.parse(from);val end=LocalDate.parse(to)
            require(!end.isBefore(start)) { "A data final deve ser igual ou posterior à inicial." }
        }
    }
    fun accepts(m:MoneyMovement,month:String):Boolean =
        (kind=="all" || m.kind==kind) && (status=="all" || m.status==status) &&
        (when(period){"month"->m.date.startsWith(month);"custom"->m.date>=from && m.date<=to;else->true}) &&
        (source=="all" ||
            source.startsWith("account:") && (m.accountId==source.removePrefix("account:") || m.toAccountId==source.removePrefix("account:")) ||
            source.startsWith("card:") && m.cardId==source.removePrefix("card:"))
}

object MunnyTransactionQuery {
    fun select(records:List<MoneyMovement>,exceptions:Set<String>,filter:TransactionFilter,
        month:String,query:String="",oldestFirst:Boolean=false):List<MoneyMovement> {
        filter.validate()
        val text=query.trim()
        val selected=records.filter{it.id !in exceptions && filter.accepts(it,month) &&
            (text.isBlank() || it.title.contains(text,true) || it.category.contains(text,true))}
        val sort=compareBy<MoneyMovement>{it.date}.thenBy{it.id}
        return selected.sortedWith(if(oldestFirst)sort else sort.reversed())
    }
    fun consumption(rows:List<MoneyMovement>)=rows.filter{it.kind=="card" || it.kind=="expense" && it.status=="paid"}.sumOf{it.amount}
    fun income(rows:List<MoneyMovement>)=rows.filter{it.kind=="income" && it.status=="paid"}.sumOf{it.amount}
    fun categories(rows:List<MoneyMovement>)=rows.filter{it.kind=="card" || it.kind=="expense" && it.status=="paid"}
        .groupBy{it.category.ifBlank{"Outros"}}.mapValues{(_,list)->list.sumOf{it.amount}}
        .toList().sortedByDescending{it.second}
    fun months(rows:List<MoneyMovement>)=rows.groupBy{YearMonth.from(LocalDate.parse(it.date)).toString()}.toSortedMap()
}

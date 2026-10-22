package app.munny.nativeapp

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Centavos em Long, sem ponto flutuante. Modelo compatível com Munny 1.4 (version:1). */
data class MoneyAccount(val id: String, val name: String, val initial: Long)
data class MoneyCard(val id: String, val name: String, val limit: Long, val closing: Int, val due: Int)
data class MoneyMovement(
    val id: String, val kind: String, val amount: Long, val date: String,
    val accountId: String = "", val toAccountId: String = "", val status: String = "planned",
    val category: String = "Outros", val title: String = "", val cardId: String = "",
    val invoiceMonth: String = "", val repeat: String = "none", val until: String = ""
)
data class MoneyBreakdown(val initial: Long, val income: Long, val expenses: Long,
                          val invoices: Long, val outgoing: Long, val incoming: Long) {
    val total: Long get() = initial + income - expenses - invoices - outgoing + incoming
}
data class MonthTotals(val income: Long, val consumption: Long)

/** Consumo é cartão + despesa paga; transferência não é despesa. */
data class SpendingChannels(
    val card:Long,val direct:Long,val transferOut:Long,val transferIn:Long,
    val invoicePayments:Long,val income:Long
) {
    val consumption:Long get()=card+direct
    val cardPercentage:Float get()=if(consumption==0L)0f else card.toFloat()/consumption.toFloat()
    val directPercentage:Float get()=if(consumption==0L)0f else direct.toFloat()/consumption.toFloat()
}
data class Invoice(val cardId: String, val month: String, val due: String, val total: Long, val paid: Long) {
    val remaining: Long get() = (total - paid).coerceAtLeast(0)
}
object FinanceMath {
    fun occurrences(records: List<MoneyMovement>, exceptions: Set<String>, through: String): List<MoneyMovement> {
        val known = records.map { it.id }.toSet()
        val result = mutableListOf<MoneyMovement>()
        for (t in records) {
            if (t.date <= through && t.id !in exceptions) result += t
            if (t.date > through || t.repeat == "none" || t.repeat.isBlank()) continue
            val start = runCatching { LocalDate.parse(t.date) }.getOrNull() ?: continue
            for (index in 1..2600) {
                val next = when (t.repeat) {
                    "weekly" -> start.plusWeeks(index.toLong())
                    "monthly" -> start.plusMonths(index.toLong())
                    "yearly" -> start.plusYears(index.toLong())
                    else -> break
                }.toString()
                if (next > through || (t.until.isNotBlank() && next > t.until)) break
                val id = "${t.id}@$next"
                if (id !in known && id !in exceptions) result += t.copy(id = id, date = next, status = "planned", repeat = "none")
            }
        }
        return result.sortedWith(compareBy<MoneyMovement> { it.date }.thenBy { it.id })
    }

    fun breakdown(accounts: List<MoneyAccount>, records: List<MoneyMovement>,
                  exceptions: Set<String>, today: String, accountId: String? = null): MoneyBreakdown {
        val initial = accounts.filter { accountId == null || it.id == accountId }.sumOf { it.initial }
        var income = 0L; var expenses = 0L; var invoices = 0L; var out = 0L; var into = 0L
        for (t in occurrences(records, exceptions, today)) {
            if (t.status != "paid" || t.kind == "card") continue
            if (accountId == null || t.accountId == accountId) {
                when (t.kind) {
                    "income" -> income += t.amount
                    "expense" -> expenses += t.amount
                    "cardPayment" -> invoices += t.amount
                    "transfer" -> out += t.amount
                }
            }
            if (t.kind == "transfer" && (accountId == null || t.toAccountId == accountId)) into += t.amount
        }
        return MoneyBreakdown(initial, income, expenses, invoices, out, into)
    }

    fun month(records: List<MoneyMovement>, exceptions: Set<String>, month: String,
              accountId: String? = null): MonthTotals {
        var income = 0L; var consumption = 0L
        for (t in occurrences(records, exceptions, YearMonth.parse(month).atEndOfMonth().toString())) {
            if (!t.date.startsWith(month) || (accountId != null && t.accountId != accountId)) continue
            if (t.kind == "income" && t.status == "paid") income += t.amount
            if ((t.kind == "expense" && t.status == "paid") || t.kind == "card") consumption += t.amount
        }
        return MonthTotals(income, consumption)
    }

    /** Mesma competência usada por month(): parcelas do cartão entram no consumo,
     * despesas apenas após confirmação; transferências internas ficam separadas.
     * Não inclui pagamento de fatura no consumo para evitar contagem dupla.
     */
    fun spendingChannels(records:List<MoneyMovement>,exceptions:Set<String>,month:String):SpendingChannels {
        val end=YearMonth.parse(month).atEndOfMonth().toString()
        var card=0L;var direct=0L;var transfers=0L;var payments=0L;var income=0L
        for(m in occurrences(records,exceptions,end)) {
            if(!m.date.startsWith(month))continue
            when(m.kind) {
                "card" -> card+=m.amount
                "expense" -> if(m.status=="paid")direct+=m.amount
                "transfer" -> if(m.status=="paid")transfers+=m.amount
                "cardPayment" -> if(m.status=="paid")payments+=m.amount
                "income" -> if(m.status=="paid")income+=m.amount
            }
        }
        // For internal transfers the same flow is a debit and a credit;
        // neither is part of consumption, and their consolidated net is zero.
        return SpendingChannels(card,direct,transfers,transfers,payments,income)
    }

    fun invoiceMonth(card: MoneyCard, purchaseDate: String): String {
        val date = LocalDate.parse(purchaseDate)
        val base = YearMonth.from(date).plusMonths(if (date.dayOfMonth > card.closing) 1 else 0)
        return base.toString()
    }
    fun due(card: MoneyCard, invoiceMonth: String): String {
        val month = YearMonth.parse(invoiceMonth).plusMonths(if (card.due <= card.closing) 1 else 0)
        return month.atDay(card.due.coerceAtMost(month.lengthOfMonth())).toString()
    }
    fun invoices(cards: List<MoneyCard>, records: List<MoneyMovement>,
                 exceptions: Set<String>, through: String): List<Invoice> {
        val cardMap = cards.associateBy { it.id }
        val bills = mutableMapOf<String, Invoice>()
        for (t in occurrences(records, exceptions, through)) {
            val card = cardMap[t.cardId] ?: continue
            val month = t.invoiceMonth.ifBlank { invoiceMonth(card, t.date) }
            val key = "${t.cardId}:$month"
            val bill = bills[key] ?: Invoice(card.id, month, due(card, month), 0, 0)
            bills[key] = when (t.kind) {
                "card" -> bill.copy(total = bill.total + t.amount)
                "cardPayment" -> if (t.status == "paid") bill.copy(paid = bill.paid + t.amount) else bill
                else -> bill
            }
        }
        return bills.values.sortedBy { it.due }
    }
    fun projected(accounts: List<MoneyAccount>, cards: List<MoneyCard>,
                  records: List<MoneyMovement>, exceptions: Set<String>, through: String,
                  accountId: String? = null): Long {
        var total = breakdown(accounts, records, exceptions, through, accountId).total
        val occurrences = occurrences(records, exceptions, through)
        for (t in occurrences) {
            if (t.status != "planned") continue
            if (accountId != null && t.accountId != accountId && t.toAccountId != accountId) continue
            when (t.kind) {
                "income" -> if (accountId == null || t.accountId == accountId) total += t.amount
                "expense" -> if (accountId == null || t.accountId == accountId) total -= t.amount
                "transfer" -> {
                    if (accountId == null) continue
                    if (t.accountId == accountId) total -= t.amount
                    if (t.toAccountId == accountId) total += t.amount
                }
            }
        }
        if (accountId == null) {
            for (bill in invoices(cards, records, exceptions, through)) if (bill.due <= through) total -= bill.remaining
        }
        return total
    }

    fun installments(base: MoneyMovement, parts: Int, firstInvoiceMonth: String): List<MoneyMovement> {
        require(parts in 1..120) { "Informe entre 1 e 120 parcelas." }
        require(base.amount > 0) { "O valor deve ser positivo." }
        val each = base.amount / parts; val remainder = base.amount % parts
        val date = LocalDate.parse(base.date)
        val month = YearMonth.parse(firstInvoiceMonth)
        return (0 until parts).map { index ->
            base.copy(id = if (parts == 1) base.id else "${base.id}-${index + 1}",
                date = date.plusMonths(index.toLong()).toString(),
                amount = each + if (index < remainder) 1 else 0,
                invoiceMonth = month.plusMonths(index.toLong()).toString(),
                status = "planned", repeat = "none")
        }
    }
}

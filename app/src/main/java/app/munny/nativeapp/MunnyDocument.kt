package app.munny.nativeapp

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** Mantém o documento version:1 idêntico ao contrato Munny web, inclusive atributos desconhecidos. */
class MunnyDocument private constructor(private val raw: JSONObject) {
    companion object {
        val DEFAULT_CATEGORIES = listOf("Alimentação","Moradia","Transporte","Compras","Lazer","Saúde","Educação","Assinaturas","Salário","Outros")
        fun blank(): MunnyDocument = parse(JSONObject().apply {
            put("version", 1)
            for (key in listOf("accounts","cards","transactions","exceptions","goals","budgets","events")) put(key, JSONArray())
            put("categories", JSONArray(DEFAULT_CATEGORIES))
            put("settings", JSONObject().put("name", "").put("theme", "system").put("onboarded", false))
        }.toString())
        fun parse(text: String): MunnyDocument {
            val obj = JSONObject(text)
            require(obj.optInt("version") == 1) { "Backup Munny incompatível (version deve ser 1)." }
            for (key in listOf("accounts","cards","transactions","exceptions","goals","budgets","events","categories")) {
                require(obj.optJSONArray(key) != null) { "Campo ausente: $key" }
                require(obj.getJSONArray(key).length() <= 100_000) { "Limite excedido: $key" }
            }
            require(obj.optJSONObject("settings") != null) { "Preferências inválidas." }
            val ids = mutableSetOf<String>()
            for (key in listOf("accounts","cards","transactions","goals","budgets","events")) {
                val rows = obj.getJSONArray(key)
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: error("Registro inválido em $key")
                    val id = item.optString("id")
                    require(id.isNotBlank() && ids.add(id)) { "ID ausente ou duplicado: $id" }
                }
            }
            return MunnyDocument(obj)
        }
    }
    fun json(): String = raw.toString()
    fun copy(): MunnyDocument = parse(json())
    private fun rows(key: String): JSONArray = raw.getJSONArray(key)
    fun objects(key: String): List<JSONObject> = rows(key).let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
    fun strings(key: String): List<String> = rows(key).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
    fun settings(): JSONObject = raw.getJSONObject("settings")
    fun accounts(): List<MoneyAccount> = objects("accounts").map { MoneyAccount(it.getString("id"),it.getString("name"),it.getLong("initial")) }
    fun cards(): List<MoneyCard> = objects("cards").map { MoneyCard(it.getString("id"),it.getString("name"),it.getLong("limit"),it.getInt("closing"),it.getInt("due")) }
    fun movements(): List<MoneyMovement> = objects("transactions").map { o ->
        MoneyMovement(id=o.getString("id"),kind=o.getString("kind"),amount=o.getLong("amount"),date=o.getString("date"),
            accountId=o.optString("accountId"),toAccountId=o.optString("toAccountId"),status=o.getString("status"),
            category=o.optString("category","Outros"),title=o.optString("title"),cardId=o.optString("cardId"),
            invoiceMonth=o.optString("invoiceMonth"),repeat=o.optString("repeat","none"),until=o.optString("until"))
    }
    fun exceptions(): Set<String> = strings("exceptions").toSet()
    fun hasContent() = listOf("accounts","cards","transactions","goals","budgets","events").any { rows(it).length() > 0 }
    fun replace(key: String, obj: JSONObject): MunnyDocument = copy().also { result ->
        val arr = JSONArray()
        for (old in result.objects(key)) if (old.getString("id") != obj.getString("id")) arr.put(old)
        arr.put(obj); result.raw.put(key, arr)
    }
    fun delete(key: String, id: String): MunnyDocument = copy().also { result ->
        val arr = JSONArray()
        for (old in result.objects(key)) if (old.getString("id") != id) arr.put(old)
        result.raw.put(key, arr)
    }
    fun rename(name: String): MunnyDocument = copy().also { it.settings().put("name", name.take(80)) }
    fun updateCategory(category: String): MunnyDocument = copy().also {
        val set = it.strings("categories").toMutableSet()
        if (category.isNotBlank()) set.add(category.take(80))
        it.raw.put("categories", JSONArray(set.toList()))
    }
    fun addAccount(name: String, amount: Long): MunnyDocument {
        require(name.isNotBlank() && amount >= 0) { "Informe uma conta e saldo inicial válido." }
        return replace("accounts",JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.take(80)).put("initial",amount).put("color","#88AA9A"))
            .also { it.settings().put("onboarded",true) }
    }
    fun upsertAccount(id: String?,name: String,amount: Long): MunnyDocument {
        require(name.isNotBlank() && amount >= 0)
        val current = objects("accounts").find { it.optString("id") == id }
        val obj = JSONObject(current?.toString() ?: "{}").put("id",id ?: UUID.randomUUID().toString())
            .put("name",name.take(80)).put("initial",amount).put("color",current?.optString("color") ?: "#88AA9A")
        return replace("accounts",obj).also { it.settings().put("onboarded",true) }
    }
    fun upsertCard(id:String?,name:String,limit:Long,closing:Int,due:Int): MunnyDocument {
        require(name.isNotBlank() && limit >= 0 && closing in 1..31 && due in 1..31)
        val current=objects("cards").find { it.optString("id") == id }
        val obj=JSONObject(current?.toString()?:"{}").put("id",id?:UUID.randomUUID().toString())
            .put("name",name.take(80)).put("limit",limit).put("closing",closing).put("due",due)
            .put("color",current?.optString("color")?:"#9BA7C8")
        return replace("cards",obj)
    }
    fun upsertMovement(m: MoneyMovement, notes: String = "", installment: Int = 1): MunnyDocument {
        require(m.title.isNotBlank() && m.amount > 0 && m.kind in setOf("income","expense","transfer","card","cardPayment"))
        LocalDate.parse(m.date)
        require(m.status in setOf("paid","planned"))
        require(m.status != "paid" || m.date <= LocalDate.now().toString()) { "Lançamento futuro deve ser previsto." }
        if (m.kind == "transfer") require(m.accountId.isNotBlank() && m.toAccountId.isNotBlank() && m.accountId != m.toAccountId)
        if (m.kind == "card") require(m.cardId.isNotBlank() && m.invoiceMonth.matches(Regex("\\d{4}-\\d{2}")))
        if (m.kind != "card") require(accounts().any { it.id == m.accountId }) { "Selecione uma conta." }
        val category=m.category.ifBlank { "Outros" }
        var base = updateCategory(category)
        val parts = if (m.kind == "card") FinanceMath.installments(m,installment,m.invoiceMonth) else listOf(m)
        for (part in parts) {
            val existing=objects("transactions").find { it.optString("id")==part.id }
            val obj=JSONObject(existing?.toString()?:"{}").put("id",part.id).put("kind",part.kind).put("title",part.title.take(150))
                .put("amount",part.amount).put("date",part.date).put("category",category)
                .put("accountId",part.accountId).put("status",part.status).put("notes",notes.take(2000))
                .put("repeat",part.repeat.ifBlank { "none" })
            if (part.toAccountId.isNotBlank())obj.put("toAccountId",part.toAccountId)
            if (part.cardId.isNotBlank())obj.put("cardId",part.cardId)
            if (part.invoiceMonth.isNotBlank())obj.put("invoiceMonth",part.invoiceMonth)
            if (part.until.isNotBlank())obj.put("until",part.until) else obj.remove("until")
            if (m.kind == "card" && existing==null) {
                obj.put("groupId",m.id)
                if (installment>1)obj.put("installment","${parts.indexOf(part)+1}/$installment")
            }
            base = base.replace("transactions",obj)
        }
        return base
    }
    fun payInvoice(card: MoneyCard, bill: Invoice,accountId: String,date:String): MunnyDocument {
        require(bill.remaining > 0 && date <= LocalDate.now().toString())
        return upsertMovement(MoneyMovement(UUID.randomUUID().toString(),"cardPayment",bill.remaining,date,
            accountId=accountId,status="paid",category="Outros",title="Pagamento · ${card.name}",cardId=card.id,invoiceMonth=bill.month))
    }
    fun addGoal(name:String,target:Long,saved:Long,deadline:String):MunnyDocument {
        require(name.isNotBlank()&&target>0&&saved>=0);LocalDate.parse(deadline)
        return replace("goals",JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.take(100))
            .put("target",target).put("saved",saved).put("deadline",deadline).put("color","#88AA9A"))
    }
    fun addBudget(category:String,amount:Long,month:String):MunnyDocument {
        require(category.isNotBlank()&&amount>0&&month.matches(Regex("\\d{4}-\\d{2}")))
        require(objects("budgets").none { it.optString("month")==month&&it.optString("category")==category })
        return updateCategory(category).replace("budgets",JSONObject().put("id",UUID.randomUUID().toString())
            .put("category",category).put("amount",amount).put("month",month))
    }
    fun addEvent(title:String,date:String,time:String,notes:String):MunnyDocument {
        require(title.isNotBlank());LocalDate.parse(date)
        return replace("events",JSONObject().put("id",UUID.randomUUID().toString()).put("title",title.take(150))
            .put("date",date).put("time",time).put("notes",notes.take(2000)).put("repeat","none"))
    }
}

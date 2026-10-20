package app.munny.nativeapp

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Documento local preservado ANTES da sync. Mutação local e CAS no servidor evitam perda silenciosa. */
class MunnyRepository(context: Context) {
    private val app = context.applicationContext
    private val file = AtomicFile(File(app.filesDir, "munny-native.json"))
    private val prefs = app.getSharedPreferences("munny_native_metadata", Context.MODE_PRIVATE)
    private val vault = TokenVault(app)
    private val endpoint = "https://eputbpsvfzywqfmuwglr.supabase.co"
    private val key = "sb_publishable_ISCaWRHypKRTqLV4N6T44g_R6GS1iy9"
    private var document = runCatching { MunnyDocument.parse(file.openRead().bufferedReader().use { it.readText() }) }
        .getOrElse { MunnyDocument.blank() }
    private var access = vault.get("access")
    private var refresh = vault.get("refresh")
    private var expiry = prefs.getLong("expiry",0L)
    var userId: String = prefs.getString("userId", "") ?: ""
        private set
    var email: String = prefs.getString("email", "") ?: ""
        private set
    private var baseOwner: String = prefs.getString("baseOwner", "") ?: ""
    private var baseRevision: Long = prefs.getLong("baseRevision",-1L)
    private var localSeq: Long = prefs.getLong("localSeq", 0L)
    var dirty: Boolean = prefs.getBoolean("dirty",false)
        private set
    var syncState: String = "Use Entrar na conta para ativar a nuvem."
        private set
    val loggedIn: Boolean get() = userId.isNotBlank() && (access.isNotBlank() || refresh.isNotBlank())
    @Synchronized fun snapshot() = document.copy()
    @Synchronized fun replace(next:MunnyDocument, needsSync:Boolean=true) {
        val bytes=next.json().toByteArray(Charsets.UTF_8)
        val stream=file.startWrite()
        try { stream.write(bytes);file.finishWrite(stream) } catch (t:Throwable) { file.failWrite(stream);throw t }
        document=next; if (needsSync) {dirty=true;localSeq++}
        prefs.edit().putBoolean("dirty",dirty).putLong("localSeq",localSeq).apply()
    }
    fun exportJson()=snapshot().json()
    fun importJson(value:String) {
        val candidate=MunnyDocument.parse(value)
        File(app.filesDir,"munny-native-pre-import-${System.currentTimeMillis()}.json").writeText(snapshot().json())
        replace(candidate,true)
    }
    private fun request(path:String,method:String="GET",body:String?=null, authenticated:Boolean=true):Pair<Int,String> {
        val conn = URL(endpoint+path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod=method;conn.connectTimeout=12000;conn.readTimeout=12000
            conn.setRequestProperty("apikey",key)
            conn.setRequestProperty("Accept","application/json")
            conn.setRequestProperty("Content-Type","application/json")
            if (authenticated) conn.setRequestProperty("Authorization","Bearer $access")
            if (body!=null) {conn.doOutput=true;conn.outputStream.bufferedWriter().use{it.write(body)}}
            val status=conn.responseCode
            val content=(if(status in 200..299)conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use {it.readText()} ?: ""
            return status to content
        } finally {conn.disconnect()}
    }
    private fun checked(response:Pair<Int,String>):String {
        if(response.first in 200..299) return response.second
        val msg=runCatching{JSONObject(response.second).let{
            it.optString("msg").ifBlank{it.optString("message").ifBlank{it.optString("error_description")}}
        }}.getOrDefault("")
        error(if(msg.isNotBlank()) "$msg (HTTP ${response.first})" else "Falha HTTP ${response.first}")
    }
    private fun setSession(json:JSONObject, fallbackEmail:String="") {
        val user=json.optJSONObject("user")
        val uid=user?.optString("id")?.takeIf{it.isNotBlank()} ?: userId
        val oldOwner=userId
        access=json.optString("access_token",access)
        refresh=json.optString("refresh_token",refresh)
        expiry=json.optLong("expires_at",0L).takeIf{it>0}?:
            (Instant.now().epochSecond+json.optLong("expires_in",3600L))
        userId=uid
        email=user?.optString("email")?.takeIf{it.isNotBlank()}?:fallbackEmail.ifBlank{email}
        vault.put("access",access);vault.put("refresh",refresh)
        prefs.edit().putString("userId",userId).putString("email",email).putLong("expiry",expiry).apply()
        if(oldOwner.isNotBlank() && oldOwner!=uid) syncState="Conta alterada: escolha a origem dos dados antes de sincronizar."
    }
    suspend fun signIn(mail:String,password:String):String=withContext(Dispatchers.IO) {
        val response=checked(request("/auth/v1/token?grant_type=password","POST",
            JSONObject().put("email",mail.trim()).put("password",password).toString(),false))
        setSession(JSONObject(response),mail.trim())
        syncState="Conta conectada. Confira qual base deseja utilizar."
        syncState
    }
    suspend fun signUp(mail:String,password:String):String=withContext(Dispatchers.IO) {
        val response=JSONObject(checked(request("/auth/v1/signup","POST",JSONObject()
            .put("email",mail.trim()).put("password",password).toString(),false)))
        if(response.has("access_token")) {setSession(response,mail.trim());"Conta criada e conectada."}
        else "Cadastro enviado. Confirme seu e-mail e depois entre na conta."
    }
    suspend fun signOut():Unit=withContext(Dispatchers.IO) {
        if(access.isNotBlank()) runCatching{request("/auth/v1/logout","POST","{}")}
        access="";refresh="";expiry=0L;userId="";email="";vault.clear()
        prefs.edit().remove("userId").remove("email").remove("expiry").apply()
        syncState="Você saiu da conta. Os arquivos locais continuam preservados."
    }
    private fun freshSession() {
        if(!loggedIn)error("Entre na sua conta primeiro.")
        if(Instant.now().epochSecond < expiry-90L && access.isNotBlank())return
        if(refresh.isBlank())error("Sessão expirada. Faça login novamente.")
        val json=JSONObject(checked(request("/auth/v1/token?grant_type=refresh_token","POST",
            JSONObject().put("refresh_token",refresh).toString(),false)))
        setSession(json)
    }
    data class Remote(val data:MunnyDocument,val revision:Long)
    private fun readRemote():Remote? {
        freshSession()
        val data=checked(request("/rest/v1/munny_documents?select=state,revision&user_id=eq.$userId"))
        val arr=JSONArray(data)
        if(arr.length()==0)return null
        val row=arr.getJSONObject(0)
        return Remote(MunnyDocument.parse(row.getJSONObject("state").toString()),row.getLong("revision"))
    }
    private fun saveRemote(currentRevision:Long):Boolean {
        freshSession()
        val seqBefore=localSeq
        val body=JSONObject().put("p_expected_revision",currentRevision).put("p_state",JSONObject(snapshot().json()))
        val result=JSONObject(checked(request("/rest/v1/rpc/munny_commit","POST",body.toString())))
        if(!result.getBoolean("ok"))return false
        baseRevision=result.getJSONObject("document").getLong("revision")
        baseOwner=userId;dirty=(localSeq!=seqBefore)
        prefs.edit().putString("baseOwner",baseOwner).putLong("baseRevision",baseRevision).putBoolean("dirty",dirty).apply()
        return true
    }
    private fun takeCloud(remote:Remote, expectedLocalSeq:Long) {
        require(localSeq==expectedLocalSeq) {"Houve uma edição local durante a sincronização. Tente novamente."}
        File(app.filesDir,"munny-native-before-cloud-${System.currentTimeMillis()}.json").writeText(snapshot().json())
        replace(remote.data,false)
        dirty=false;baseOwner=userId;baseRevision=remote.revision
        prefs.edit().putBoolean("dirty",false).putString("baseOwner",baseOwner).putLong("baseRevision",baseRevision).apply()
    }
    /** Nenhuma escolha destrutiva é tomada automaticamente quando local e nuvem diferem. */
    suspend fun sync(mode:String="auto"):String=withContext(Dispatchers.IO) {
        freshSession()
        val remote=readRemote()
        val current=snapshot()
        val seqBefore=localSeq
        val ownerMatches=(baseOwner==userId && baseRevision>=0)
        when(mode) {
            "cloud" -> {
                if(remote==null)error("Não existem registros nesta conta.")
                takeCloud(remote,seqBefore);syncState="Dados da conta restaurados no aparelho."
            }
            "local" -> {
                if(!saveRemote(remote?.revision?:0L))error("A nuvem mudou enquanto você enviava. Tente novamente.")
                syncState="Dados deste aparelho enviados com segurança."
            }
            else -> {
                when {
                    ownerMatches && remote!=null && remote.revision==baseRevision && dirty -> {
                        if(!saveRemote(baseRevision))error("Conflito de versões. Escolha a base em Configurações.")
                        syncState="Alterações deste aparelho sincronizadas."
                    }
                    ownerMatches && remote!=null && remote.revision==baseRevision -> syncState="Dispositivos em dia."
                    ownerMatches && remote!=null && !dirty -> {
                        takeCloud(remote,seqBefore);syncState="Mudanças da nuvem recebidas."
                    }
                    ownerMatches && remote!=null && dirty -> syncState="Conflito: alterações locais e na nuvem. Escolha explicitamente qual base manter."
                    remote==null && !current.hasContent() -> syncState="Conta vazia. Crie sua primeira conta ou lance seus valores iniciais."
                    remote==null -> syncState="Dados locais sem cópia na nuvem. Use Enviar aparelho."
                    !current.hasContent() -> {takeCloud(remote,seqBefore);syncState="Dados da nuvem recebidos."}
                    else -> syncState="Existem dados locais e na nuvem. Escolha qual base manter em Configurações."
                }
            }
        }
        syncState
    }
}

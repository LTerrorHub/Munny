package app.munny.nativeapp

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MunnyViewModel(application:Application):AndroidViewModel(application) {
    private val repo=MunnyRepository(application)
    var data by androidx.compose.runtime.mutableStateOf(repo.snapshot());private set
    var notice by androidx.compose.runtime.mutableStateOf("");private set
    var busy by androidx.compose.runtime.mutableStateOf(false);private set
    var cloudStatus by androidx.compose.runtime.mutableStateOf(repo.syncState);private set
    private val displayPrefs=application.getSharedPreferences("munny_visual_settings", Context.MODE_PRIVATE)
    var balancesVisible by androidx.compose.runtime.mutableStateOf(displayPrefs.getBoolean("balances_visible",true))
        private set
    var matrixEnabled by androidx.compose.runtime.mutableStateOf(displayPrefs.getBoolean("matrix_enabled",true))
        private set
    var themeMode by androidx.compose.runtime.mutableStateOf(displayPrefs.getString("theme_mode","system") ?: "system")
        private set
    var themeTone by androidx.compose.runtime.mutableStateOf(displayPrefs.getString("theme_tone","mint") ?: "mint")
        private set
    var homeOrder by androidx.compose.runtime.mutableStateOf(
        HomeModules.normalize(displayPrefs.getString("home_order","")!!.split(",")))
        private set
    var homeHidden by androidx.compose.runtime.mutableStateOf(
        displayPrefs.getString("home_hidden","")!!.split(",").filter { it in HomeModules.ids }.toSet())
        private set
    fun updateThemeTone(tone:String) {
        if(tone !in setOf("mint","ocean","violet","amber"))return
        themeTone=tone
        displayPrefs.edit().putString("theme_tone",tone).apply()
    }
    fun updateHomeLayout(order:List<String>,hidden:Set<String>) {
        homeOrder=HomeModules.normalize(order)
        homeHidden=hidden.intersect(HomeModules.ids.toSet())
        displayPrefs.edit().putString("home_order",homeOrder.joinToString(","))
            .putString("home_hidden",homeHidden.joinToString(",")).apply()
    }
    // Widgets never display monetary values until the user explicitly opts in.
    var widgetAmountsVisible by androidx.compose.runtime.mutableStateOf(
        displayPrefs.getBoolean("widget_amounts_visible",false)
    )
        private set
    fun updateWidgetAmountsVisible(enabled:Boolean) {
        widgetAmountsVisible=enabled
        displayPrefs.edit().putBoolean("widget_amounts_visible",enabled).apply()
        MunnyWidgets.refreshAll(getApplication())
    }
    fun updateThemeMode(mode:String) {
        if(mode !in listOf("system","light","dark"))return
        themeMode=mode
        displayPrefs.edit().putString("theme_mode",mode).apply()
        MunnyWidgets.refreshAll(getApplication())
    }
    fun toggleBalanceVisibility(){
        balancesVisible=!balancesVisible
        displayPrefs.edit().putBoolean("balances_visible",balancesVisible).apply()
        MunnyWidgets.refreshAll(getApplication())
    }
    fun updateMatrixEnabled(enabled:Boolean){matrixEnabled=enabled;displayPrefs.edit().putBoolean("matrix_enabled",enabled).apply()}
    val email get()=repo.email
    val loggedIn get()=repo.loggedIn
    var activeTab by androidx.compose.runtime.mutableStateOf("home")
    private var syncJob:Job?=null
    fun inform(msg:String){notice=msg}
    fun mutate(block:(MunnyDocument)->MunnyDocument):Boolean {
        try {
            val next=block(data)
            repo.replace(next)
            data=next
            MunnyWidgets.refreshAll(getApplication())
            notice="Registro salvo neste aparelho."
            if(loggedIn) {
                syncJob?.cancel()
                syncJob=viewModelScope.launch {delay(1200);performSync("auto",silent=true)}
            }
            return true
        } catch(t:Throwable){notice=t.message?:"Não foi possível salvar."; return false}
    }
    fun importJson(text:String) {
        try {repo.importJson(text);data=repo.snapshot();MunnyWidgets.refreshAll(getApplication())
            notice="Backup JSON importado. Confira o saldo antes de sincronizar."}
        catch (t:Throwable){notice=t.message?:"Backup inválido."}
    }
    fun exportJson()=repo.exportJson()
    fun authenticate(email:String,password:String,signup:Boolean) = viewModelScope.launch {
        busy=true
        try {
            val result=if(signup) repo.signUp(email,password) else repo.signIn(email,password)
            cloudStatus=result;notice=result
            if(repo.loggedIn)performSync("auto",silent=true)
        }catch(t:Throwable){notice=t.message?:"Falha na autenticação."}
        finally{busy=false}
    }
    fun logout()=viewModelScope.launch {
        busy=true
        try {repo.signOut();cloudStatus=repo.syncState;notice="Sessão encerrada. Dados locais preservados."}
        catch(t:Throwable){notice=t.message?:"Não foi possível sair."}
        finally{busy=false}
    }
    fun performSync(mode:String="auto",silent:Boolean=false)=viewModelScope.launch {
        if(!loggedIn)return@launch
        busy=true
        try {cloudStatus=repo.sync(mode);data=repo.snapshot()
            MunnyWidgets.refreshAll(getApplication())
            if(!silent)notice=cloudStatus}
        catch(t:Throwable){cloudStatus="Erro na sincronização: ${t.message}";if(!silent)notice=cloudStatus}
        finally{busy=false}
    }
}

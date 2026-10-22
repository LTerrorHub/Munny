package app.munny.nativeapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.AtomicFile
import android.view.View
import android.widget.RemoteViews
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Widgets are local, read-only and PRIVATE by default, even on unlocked launchers. */
abstract class MunnyWidgetProvider:AppWidgetProvider() {
    protected abstract val widgetType:String
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {
        ids.forEach { id -> MunnyWidgets.update(context,manager,id,widgetType) }
    }
    override fun onEnabled(context:Context) { MunnyWidgets.refreshAll(context) }
}
class MunnyBalanceWidget:MunnyWidgetProvider(){override val widgetType="balance"}
class MunnySpendingWidget:MunnyWidgetProvider(){override val widgetType="spending"}
class MunnyInvoicesWidget:MunnyWidgetProvider(){override val widgetType="invoices"}

object MunnyWidgets {
    private fun readLocal(context:Context):MunnyDocument {
        val file=AtomicFile(File(context.filesDir,"munny-native.json"))
        return runCatching {
            MunnyDocument.parse(file.openRead().bufferedReader(Charsets.UTF_8).use{it.readText()})
        }.getOrElse{MunnyDocument.blank()}
    }

    private fun amountsAllowed(context:Context):Boolean {
        val prefs=context.getSharedPreferences("munny_visual_settings",Context.MODE_PRIVATE)
        return prefs.getBoolean("widget_amounts_visible",false) &&
            prefs.getBoolean("balances_visible",true)
    }

    fun refreshAll(context:Context) {
        val manager=AppWidgetManager.getInstance(context)
        listOf(
            MunnyBalanceWidget::class.java to "balance",
            MunnySpendingWidget::class.java to "spending",
            MunnyInvoicesWidget::class.java to "invoices"
        ).forEach { (provider,type) ->
            manager.getAppWidgetIds(ComponentName(context,provider)).forEach { id ->
                update(context,manager,id,type)
            }
        }
    }

    fun update(context:Context,manager:AppWidgetManager,id:Int,type:String) {
        val darkMode=when(context.getSharedPreferences("munny_visual_settings",Context.MODE_PRIVATE)
            .getString("theme_mode","system")) {
            "dark"->true
            "light"->false
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        val bg=if(darkMode)R.drawable.munny_widget_dark else R.drawable.munny_widget_light
        val ink=if(darkMode)0xFFF0FAF6.toInt() else 0xFF18312C.toInt()
        val muted=if(darkMode)0xFFACBDB8.toInt() else 0xFF667A73.toInt()
        val data=readLocal(context)
        val canShow=amountsAllowed(context)
        val hidden="R$ ••••••"
        fun value(v:Long)=if(canShow)money(v)else hidden
        val now=YearMonth.now()
        val ch=FinanceMath.spendingChannels(data.movements(),data.exceptions(),now.toString())
        val view=RemoteViews(context.packageName,R.layout.munny_widget_card)
        view.setInt(R.id.munny_widget_root,"setBackgroundResource",bg)
        view.setTextColor(R.id.munny_widget_title,ink)
        view.setTextColor(R.id.munny_widget_value,ink)
        view.setTextColor(R.id.munny_widget_hint,muted)
        view.setTextColor(R.id.munny_widget_footer,muted)
        val title:String;val main:String;val hint:String;val footer:String;val progress:Int?
        when(type) {
            "balance" -> {
                val cash=FinanceMath.breakdown(
                    data.accounts(),data.movements(),data.exceptions(),LocalDate.now().toString()
                ).total
                title="Munny · Saldo disponível"
                main=value(cash)
                hint="Nas suas contas, sem compras de cartão não pagas"
                footer="Mês: ${value(ch.income)} recebidos · ${value(ch.consumption)} consumidos"
                progress=null
            }
            "spending" -> {
                title="Munny · Tipos de gasto"
                main=value(ch.consumption)
                hint="Cartão: ${String.format(java.util.Locale("pt","BR"),"%.0f",ch.cardPercentage*100f)}% · direto: ${String.format(java.util.Locale("pt","BR"),"%.0f",ch.directPercentage*100f)}%"
                footer="Transferências internas: ${value(ch.transferOut)}"
                progress=(ch.cardPercentage*100f).roundToInt()
            }
            else -> {
                val due=FinanceMath.invoices(data.cards(),data.movements(),data.exceptions(),
                    LocalDate.now().plusYears(10).toString()).filter{it.remaining>0L}
                title="Munny · Faturas em aberto"
                main=value(due.sumOf{it.remaining})
                val next=due.filter{it.due>=LocalDate.now().toString()}.minByOrNull{it.due}
                val cardName=next?.let{entry->data.cards().find{it.id==entry.cardId}?.name}
                hint=if(next==null)"Sem faturas pendentes" else "Próxima: ${next.due} · ${cardName?:"Cartão"}"
                footer="Inclui parcelas futuras · toque para abrir Munny"
                progress=null
            }
        }
        view.setTextViewText(R.id.munny_widget_title,title)
        view.setTextViewText(R.id.munny_widget_value,main)
        view.setTextViewText(R.id.munny_widget_hint,hint)
        view.setTextViewText(R.id.munny_widget_footer,footer)
        view.setViewVisibility(R.id.munny_widget_progress,
            if(progress==null)View.GONE else View.VISIBLE)
        if(progress!=null)view.setProgressBar(R.id.munny_widget_progress,100,progress,false)
        val intent=Intent(context,MainActivity::class.java).apply {
            flags=Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val click=PendingIntent.getActivity(context,id,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        view.setOnClickPendingIntent(R.id.munny_widget_root,click)
        manager.updateAppWidget(id,view)
    }
}

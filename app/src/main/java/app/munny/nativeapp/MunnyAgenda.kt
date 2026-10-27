package app.munny.nativeapp

import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Separate native agenda: month calendar, selected-day events and event registration. */
@Composable fun MunnyAgendaScreen(data:MunnyDocument,onNewEvent:(String)->Unit,onEditEvent:(String)->Unit) {
    var selectedDate by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(today) }
    val selected=LocalDate.parse(selectedDate)
    var monthValue by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(YearMonth.from(selected).toString()) }
    val month=YearMonth.parse(monthValue)
    val events=data.objects("events").sortedWith(compareBy<JSONObject>({it.optString("date")},{it.optString("time")}))
    val eventDays=events.map{it.optString("date")}.toSet()
    val start=month.atDay(1).minusDays((month.atDay(1).dayOfWeek.value-1).toLong())
    val selectedEvents=events.filter{it.optString("date")==selected.toString()}
    val upcoming=events.filter{it.optString("date")>selected.toString()}.take(12)
    LazyColumn(contentPadding=PaddingValues(16.dp,15.dp,16.dp,110.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {
            Text("Agenda",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            Text("Consulte e edite eventos de qualquer data, passada ou futura.",color=Muted)
        }
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
            TextButton(onClick={selectedDate=today;monthValue=YearMonth.now().toString()}){Text("Hoje")}
        }}
        item { SheetCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={
                    val next=month.minusMonths(1);monthValue=next.toString()
                    selectedDate=next.atDay(minOf(selected.dayOfMonth,next.lengthOfMonth())).toString()
                }) {
                    Icon(Icons.Default.ChevronLeft,"Mês anterior",tint=Sage)
                }
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(month.month.getDisplayName(TextStyle.FULL,Locale("pt","BR"))
                        .replaceFirstChar{it.uppercase()}+" "+month.year,
                        fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                    Text("Toque em um dia para visualizar",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick={
                    val next=month.plusMonths(1);monthValue=next.toString()
                    selectedDate=next.atDay(minOf(selected.dayOfMonth,next.lengthOfMonth())).toString()
                }) {
                    Icon(Icons.Default.ChevronRight,"Próximo mês",tint=Sage)
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){
                listOf("Seg","Ter","Qua","Qui","Sex","Sáb","Dom").forEach { name ->
                    Text(name,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,
                        color=Muted,style=MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            repeat(6){ week ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    repeat(7){ weekday ->
                        val day=start.plusDays((week*7+weekday).toLong())
                        val active=day==selected
                        val within=YearMonth.from(day)==month
                        val hasEvent=day.toString() in eventDays
                        val isToday=day==LocalDate.now()
                        Surface(onClick={selectedDate=day.toString();monthValue=YearMonth.from(day).toString()},
                            modifier=Modifier.weight(1f).height(46.dp),
                            shape=RoundedCornerShape(12.dp),
                            color=when {
                                active->SageSoft
                                isToday->MaterialTheme.colorScheme.surfaceVariant
                                else->MaterialTheme.colorScheme.surface
                            }) {
                            Column(horizontalAlignment=Alignment.CenterHorizontally,
                                verticalArrangement=Arrangement.Center) {
                                Text(day.dayOfMonth.toString(),
                                    color=when{active->Sage;within->Ink;else->Muted.copy(alpha=.52f)},
                                    fontWeight=if(active||isToday)FontWeight.Bold else FontWeight.Normal,
                                    style=MaterialTheme.typography.labelMedium)
                                if(hasEvent) {
                                    Text("●",color=if(active)Sage else MatrixOrange,
                                        style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
            }
        }}
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Eventos de "+selected.dayOfMonth+"/"+selected.monthValue,
                        style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                    Text(if(selectedEvents.isEmpty())"Sem eventos nesta data"
                        else selectedEvents.size.toString()+" evento(s) neste dia",color=Muted)
                }
                Button(onClick={onNewEvent(selected.toString())}) {
                    Icon(Icons.Default.Add,contentDescription=null,modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Evento")
                }
            }
        }
        if(selectedEvents.isEmpty())item{SheetCard{Text("Planeje algo novo para esta data.",color=Muted)}}
        items(selectedEvents,key={it.optString("id")}) { event ->
            SheetCard {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Event,contentDescription=null,tint=Sage)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(event.optString("title"),fontWeight=FontWeight.SemiBold)
                        Text(event.optString("time").ifBlank{"Horário livre"},color=Muted)
                    }
                }
                if(event.optString("notes").isNotBlank())
                    Text(event.optString("notes"),color=Muted,style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={onEditEvent(event.optString("id"))}){Icon(Icons.Default.Edit,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Editar evento")}
            }
        }
        if(upcoming.isNotEmpty()) {
            item {
                Text("Próximos eventos",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            }
            items(upcoming,key={it.optString("id")}) { event ->
                SheetCard {
                    Text(event.optString("title"),fontWeight=FontWeight.SemiBold)
                    Text(event.optString("date")+" · "+event.optString("time"),color=Muted)
                    TextButton(onClick={onEditEvent(event.optString("id"))}){Text("Editar evento")}
                }
            }
        }
    }
}

package app.munny.nativeapp

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MunnyChartShare(val key:String,val label:String,val amount:Long)
data class MunnyTrendPoint(val date:String,val income:Long,val consumption:Long)
data class MunnyTrend(val points:List<MunnyTrendPoint>,val daily:Boolean,val omitted:Int=0)

object MunnyChartMath {
    fun safeTopic(topic:String)=topic.takeIf{it in setOf("trend","categories","channels")}?:"categories"
    fun safeStyle(topic:String,style:String)=if(topic=="trend")style.takeIf{it in setOf("line","bars")}?:"line"
        else style.takeIf{it in setOf("donut","bars")}?:"donut"
    fun shares(rows:List<MoneyMovement>,topic:String,limit:Int=Int.MAX_VALUE):List<MunnyChartShare> {
        require(limit>=2)
        val groups=if(topic=="channels")listOf(
            MunnyChartShare("card","Cartão",rows.filter{it.kind=="card"}.sumOf{it.amount}),
            MunnyChartShare("direct","Despesa direta",rows.filter{it.kind=="expense"&&it.status=="paid"}.sumOf{it.amount})
        ).filter{it.amount>0L} else MunnyTransactionQuery.categories(rows).map{(name,value)->MunnyChartShare("category:$name",name,value)}
        if(groups.size<=limit)return groups
        val first=groups.take(limit-1)
        return first+MunnyChartShare("__other__","Outras categorias",groups.drop(limit-1).sumOf{it.amount})
    }
    /** True zeroes fill missing dates. Straight lines never invent intermediate overshoots. */
    fun trend(rows:List<MoneyMovement>,maxMonths:Int=12):MunnyTrend {
        require(maxMonths>0)
        if(rows.isEmpty())return MunnyTrend(emptyList(),true)
        val first=LocalDate.parse(rows.minOf{it.date})
        val last=LocalDate.parse(rows.maxOf{it.date})
        val daily=YearMonth.from(first)==YearMonth.from(last)
        val grouped=rows.groupBy{if(daily)it.date else it.date.take(7)}
        val all=if(daily) (0..ChronoUnit.DAYS.between(first,last)).map{first.plusDays(it).toString()}
            else {
                val start=YearMonth.from(first);val end=YearMonth.from(last)
                (0..ChronoUnit.MONTHS.between(start,end)).map{start.plusMonths(it).toString()}
            }
        val dates=if(daily)all else all.takeLast(maxMonths)
        return MunnyTrend(dates.map{date->
            val entries=grouped[date].orEmpty()
            MunnyTrendPoint(date,MunnyTransactionQuery.income(entries),MunnyTransactionQuery.consumption(entries))
        },daily,all.size-dates.size)
    }
}

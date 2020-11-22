package furhatos.app.taxibot.nlu
import furhatos.nlu.*
import furhatos.nlu.grammar.Grammar
import furhatos.nlu.kotlin.grammar
import furhatos.nlu.common.Number
import furhatos.nlu.wikidata.City
import furhatos.util.Language


class confirmlocation (
    val departure: City?= null,
    val destination: City? = null
    ) :Intent(){

    override fun getExamples(lang: Language): List<String> {
        return listOf("I want to go from @departure to @destination",
                "I want to go to @destination",
                "@destination")
    }

}

class data(
        val distance: Nothing? = null,
        val perkm:Nothing?=null,
        val cost: Nothing?=null
        ){
    fun getDistance(){
        //google API call here
    }
    fun getCost(){
        //cost calculation here
    }
}
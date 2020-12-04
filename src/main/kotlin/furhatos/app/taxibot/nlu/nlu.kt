package furhatos.app.taxibot.nlu
import furhatos.nlu.*
import furhatos.nlu.grammar.Grammar
import furhatos.nlu.kotlin.grammar
import furhatos.nlu.common.Number
import furhatos.nlu.wikidata.City
import furhatos.util.Language

// Wildcard-based: has problems because of greedy matching
// i.e., I want to go from Munich Library to KTH Stockholm becomes
//       departure: null
//       destination: go from Munih Library to KTH Stockholm (because of "to" right before)
//class DepartureEntity: WildcardEntity("departure", TravelRequest())
class RequestEntity: WildcardEntity("request", TravelRequest())

class TravelRequest (
    val request: RequestEntity? = null
    ) :Intent(){

    override fun getExamples(lang: Language): List<String> {
        return listOf("I want to go @request",
                "I would like to go @request",
                "@request")
    }
}

class Price (
    val customer_bid: Number? = null
    ) :Intent(){

    override fun getExamples(lang: Language): List<String> {
        return listOf("@customer_bid",
                "My lowest is @customer_bid",
                "I can pay @customer_bid")
    }
}


//class TravelRequest (
//        var departure: City? = null,
//        val destination: City? = null
//    ) :Intent() {
//
//    override fun getExamples(lang: Language): List<String> {
//        return listOf("I want to go from @departure to @destination",
//                "I want to go to @destination.",
//                "from @departure to @destination",
//                "to @destination",
//                "@destination")
//    }
//}

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
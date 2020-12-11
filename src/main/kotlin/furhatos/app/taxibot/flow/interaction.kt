package furhatos.app.taxibot.flow

import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.app.taxibot.nlu.*
import furhatos.nlu.wikidata.City
import furhatos.app.taxibot.APP_ID
import furhatos.nlu.Intent
import khttp.get
import kotlin.math.roundToInt
import kotlin.random.Random

val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
var distance: Int? = null
var duration: Int? = null
var cost: Int=0
var accepting_price: Int=0
var accepted_bid : Int=0
var bargain_counter : Int=0
var bargain_2_counter : Int=0
var current_bid : Int=0

var departure: String?= null
var destination: String?= null

val Start : State = state(Interaction) {
    onEntry {
        furhat.say("Hi there.")
        goto(getlocation)
    }
}

val getlocation : State = state(Interaction) {

    onEntry {
        furhat.ask("Where do you want to go? I can recommend Stockholm City Hall, Stockholm University, or maybe the Vasa Museeum.")
    }

    onResponse<TravelRequest> {
        // parse request string into destination and departure
        var request_string = it.intent.request.toString()
        println(it.intent.request)
        var destination_regex = "(?:.*to )?(?<des>.+?(?= from|$)).*".toRegex()
        var departure_regex = "(?<dep>(?<=from ).*?(?= to|\$)|(?<=^)(?!from ).*?(?= to))".toRegex()
        val dest_match = destination_regex.find(request_string)
        if (dest_match != null) {
            destination = dest_match.groups["des"]?.value
        }
        else {
            furhat.ask("Sorry, can you repeat that?")
        }
        val dep_match = departure_regex.find(request_string)
        if (dep_match != null) {
            departure = dep_match.groups["dep"]?.value
        }
        else {
            departure = "KTH Stockholm"
        }

        // generate query
        val departure_query = departure?.replace(' ', '+')
        val destination_query = destination?.replace(' ', '+')
        val query = "$BASE_URL?origin=$departure_query&destination=$destination_query&key=$APP_ID"
        println(query)
        val response = get(query)

        if (response.statusCode != 200){
            furhat.say("I will take you to Gothenburg.")
            departure = "Stockholm"
            departure = "Gothenburg"
            println(response.statusCode)
            goto(tellprice)
        }
        else if(response.jsonObject.getJSONArray("routes").length() == 0) {
            furhat.ask("I can't find a way from $departure to $destination. Maybe try a more well known landmark instead?")
        }
        var route_info = response.jsonObject.getJSONArray("routes")
                .getJSONObject(0)
                .getJSONArray("legs")
                .getJSONObject(0)

        // extract useful data
        distance = route_info.getJSONObject("distance").getInt("value") // in meters
        val distance_text = route_info.getJSONObject("distance").getString("text")
        duration = route_info.getJSONObject("duration").getInt("value") // in seconds
        val duration_text = route_info.getJSONObject("duration").getString("text")
        val start_address_text = route_info.getString("start_address")
        val end_address_text = route_info.getString("end_address")
        cost= distance!!/1000 *12 + duration!!/3600 *20
        accepting_price = (0.6 * cost).roundToInt()
        accepted_bid=0
        bargain_counter=0
        bargain_2_counter=0
        current_bid = 0

        println(start_address_text)
        println(end_address_text)
        println(distance_text)
        println(duration_text)
        println(cost)

        goto(confirmdest)
    }
}

val confirmdest : State= state(Interaction){
    var time_hours = duration?.div(3600)
    var time_mins= (duration?.rem(3600))?.div(60)
    var dis_km= distance?.div(1000)
    onEntry {
        furhat.ask("You want to go from $departure to $destination right? ")
    }

    onResponse<Yes>{

        furhat.say("It's $dis_km kilometers away and would take $time_hours hours and $time_mins minutes")
        goto(tellprice)
    }

    onResponse<No>{
        goto(getlocation)

    }

}


val tellprice : State = state(Interaction) {

    onEntry {
        furhat.ask("It costs $cost kronor. Is that ok?")
    }

    onResponse<Yes> {
        furhat.say("That's great, I'm glad you didn't try to bargain. Let's go.")
    }

    onResponse<No>{
        goto(bargain)
    }
}

val bargain : State = state(Interaction) {
    onEntry {
        cost= (cost - 0.1 * Random.nextFloat() * cost).roundToInt() - 1
        furhat.ask("How about $cost kronor?")
    }
    onReentry {
        random(
        {furhat.ask("How about $cost kronor?")},
        {furhat.ask("Can we agree on $cost kronor?")})
    }

    onResponse<Yes> {
        furhat.say("That's great. Let's go.")
    }

    onResponse<No>{
        if (Random.nextFloat() <= 0.5 && bargain_2_counter<1) {
            bargain_2_counter= bargain_2_counter+1
            cost= (cost - 0.1 * Random.nextFloat() * cost).roundToInt() - 1
            reentry()
        } else {
            goto(last_bargain)
        }
    }
}


val last_bargain : State = state(Interaction) {
    onEntry {
        furhat.ask("Well, what price are you willing to pay? I might be able to go just a tiny bit lower.")
    }
    onReentry {
        random(
        {furhat.ask("To travel $distance meters you have to pay a bit. What is your final offer?")},
        {furhat.ask("I think you can pay a bit more! What can you afford at most?")})
    }
    onResponse<Price> {
        if (bargain_counter <= 3) {
            var customer_bid = it.intent.customer_bid.toString().toIntOrNull()
            if (customer_bid == null) {
                current_bid = 0
                furhat.ask("Sorry, I did not understand that. How much are you willing to pay?")
            }
            else {
                bargain_counter = bargain_counter + 1
                println(customer_bid)

                if (customer_bid >= accepting_price) {
                    accepted_bid = customer_bid
                    current_bid = customer_bid
                    println(accepted_bid)
                    furhat.ask("$customer_bid. Is that ok?")
                } else {
                    if (accepted_bid > 0) {
                        current_bid = accepted_bid
                        furhat.ask("$customer_bid? No. But $accepted_bid was OK. shall we close the deal on that? ")
                    } else {
                        cost = (cost - 0.1 * Random.nextFloat() * cost).roundToInt() - 1
                        cost = maxOf(cost, accepting_price)
                        current_bid = cost
                        furhat.ask("$customer_bid? No. How about $cost. Shall we close the deal on that?")
                    }
                }
            }

        }
        else{
            furhat.say("Sorry, I am tired of haggling. Good bye!")
        }
    }
    onResponse<Yes> {
        if (current_bid != 0) {
            furhat.say("Sure. Let's go.")
        }
        else {
            furhat.ask("Sorry, I did not understand that. How much are you willing to pay?")
        }
    }
    onResponse<No>{
        if (current_bid == 0) {
            furhat.ask("Sorry, I did not understand that. How much are you willing to pay?")
        }
        else if (bargain_counter <= 3) {
            bargain_counter = bargain_counter + 1
            reentry()
        } else {
            furhat.say("Sorry, I am tired of haggling. Good bye!")
        }
    }
}

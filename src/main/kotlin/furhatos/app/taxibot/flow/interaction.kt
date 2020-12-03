package furhatos.app.taxibot.flow

import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.app.taxibot.nlu.*
import furhatos.nlu.wikidata.City
import furhatos.app.taxibot.APP_ID
import furhatos.nlu.Intent
import khttp.get
import kotlin.math.roundToInt


val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
var distance: Int? = null
var duration: Int? = null
var cost: Int=0
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
        furhat.ask("Where do you want to go?")
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
            furhat.ask("I did not understand that. Can you say it again?")
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
            furhat.ask("I can't find a way from $departure to $destination. Could you be more specific?")
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
        cost= distance!! /1000!! * 12 + duration!! /3600!! *20


        println(start_address_text)
        println(end_address_text)
        println(distance_text)
        println(duration_text)
        println(cost!!)

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
        furhat.say("That's great. Let's go.")
    }

    onResponse<No>{
        goto(bargain)
    }
}

val bargain : State = state(Interaction) {
    var new_cost= (cost - 0.1 * cost).roundToInt()

    onEntry {
        furhat.ask("How about $new_cost kronor?")
    }

    onResponse<Yes> {
        furhat.say("That's great. Let's go.")
    }

    onResponse<No>{
        furhat.say("I'm sorry, but I cannot bargain more. Thank you and have a great day!")
    }
}



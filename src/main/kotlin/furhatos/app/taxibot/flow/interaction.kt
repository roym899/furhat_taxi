package furhatos.app.taxibot.flow

import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.app.taxibot.nlu.*
import furhatos.nlu.wikidata.City
import furhatos.app.taxibot.APP_ID
import furhatos.nlu.Intent
import khttp.get

val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"
var distance: Int? = null
var duration: Int? = null
var cost: Int?=null
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
        val travel_request = it.intent
        if (travel_request.destination == null) {
            furhat.ask("I did not understand that. Can you say it again?")
        }
        else {
            // make sure both departure and destination are defined
            if (travel_request.departure == null) {
                travel_request.departure = City(name="Stockholm")
            }

            // generate query
            departure = travel_request.departure.toString().replace(' ', '+')
            destination = travel_request.destination.toString().replace(' ', '+')
            val query = "$BASE_URL?origin=$departure&destination=$destination&key=$APP_ID"
            val response = get(query)
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


            // TODO error handling API if sth went wrong (see furhat API tutorial on error handling)

            goto(confirmdest)
        }
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
    var new_cost= cost?.minus(50)

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



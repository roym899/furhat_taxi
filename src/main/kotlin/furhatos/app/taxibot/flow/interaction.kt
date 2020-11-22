package furhatos.app.taxibot.flow

import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.app.taxibot.nlu.*
import furhatos.nlu.wikidata.City
import furhatos.app.taxibot.APP_ID

val BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"

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
            val departure = travel_request.departure.toString().replace(' ', '+')
            val destination = travel_request.destination.toString().replace(' ', '+')
            val query = "$BASE_URL?origin=$departure&destination=$destination&key=$APP_ID"
            val response = khttp.get(query)
            var route_info = response.jsonObject.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)

            // extract useful data
            val distance = route_info.getJSONObject("distance").getInt("value") // in meters
            val distance_text = route_info.getJSONObject("distance").getString("text")
            val duration = route_info.getJSONObject("duration").getInt("value") // in seconds
            val duration_text = route_info.getJSONObject("duration").getString("text")
            val start_address_text = route_info.getString("start_address")
            val end_address_text = route_info.getString("end_address")
            println(start_address_text)
            println(end_address_text)
            println(distance_text)
            println(duration_text)

            // TODO: confirm departure and destination ?
            // TODO error handling API if sth went wrong (see furhat API tutorial on error handling)
            // TODO: store departure and destination and API data
            goto(tellprice)
        }
    }
}

val tellprice : State = state(Interaction) {

    onEntry {
        furhat.ask("It costs x krona. Is that ok?")
    }

    onResponse<Yes> {
        furhat.say("That's great. Let's go.")
    }

    onResponse<No>{
        goto(bargain)
    }
}

val bargain : State = state(Interaction) {

    onEntry {
        furhat.ask("How about Y krona?")
    }

    onResponse<Yes> {
        furhat.say("That's great. Let's go.")
    }

    onResponse<No>{
        furhat.say("I'm sorry, but I cannot bargain more. Thank you and have a great day!")
    }
}



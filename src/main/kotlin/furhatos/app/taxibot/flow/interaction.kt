package furhatos.app.taxibot.flow

import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.app.taxibot.nlu.*

val Start : State = state(Interaction) {

    onEntry {
        furhat.say("Hi there")

        goto(getlocation)
    }


}

val getlocation : State = state(Interaction) {

    onEntry {
        furhat.ask("Where do you want to go")
    }

    onResponse<confirmlocation> {
        goto(tellprice)
    }

}

val tellprice : State = state(Interaction) {

    onEntry {
        furhat.ask("It costs x Krones Are you ok?")
    }


    onResponse<Yes> {
        furhat.say("That's great lets go")
    }

    onResponse<No>{
        goto(bargain)
    }

}

val bargain : State = state(Interaction) {

    onEntry {
        furhat.ask("Are you ok with Y krones?")
    }

    onResponse<Yes> {
        furhat.say("That's great lets go")
    }

    onResponse<No>{
        furhat.say("I'm sorry I cannot bargain more, Thank you and have a great day")
    }

}



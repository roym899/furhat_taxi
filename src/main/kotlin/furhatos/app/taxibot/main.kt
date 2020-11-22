package furhatos.app.taxibot

import furhatos.app.taxibot.flow.*
import furhatos.skills.Skill
import furhatos.flow.kotlin.*

class TaxiBotSkill : Skill() {
    override fun start() {
        Flow().run(Idle)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}

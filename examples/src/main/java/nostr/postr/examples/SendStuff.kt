package nostr.postr.examples

import nostr.postr.*
import nostr.postr.events.EncryptedDmEvent
import nostr.postr.events.Event
import nostr.postr.events.MetadataEvent
import nostr.postr.events.TextNoteEvent
import java.util.*
import org.spongycastle.util.encoders.Hex

class SendStuff {
    companion object {
        private val persona = Persona(Hex.decode("ed677a60034a04bb282e1b4587e1ece5c5b81e2261d7aeea933c0ee07095df80"))

        private val listener = object: Client.Listener() {
            override fun onNewEvent(event: Event) {
                if (event.pubKey.toHex() == persona.publicKey.toHex()) {
                    logDetail(event, event.toJson())
                    stop()
                } else {
                    logDetail(event, "Why do we get this event? ${event.id}")
                }
            }
        }

        @JvmStatic
        fun main(vararg args: String) {
            println("""Persona(privKey:${persona.privateKey!!.toHex()}, pubKey:${persona.publicKey.toHex()})""")
            Client.subscribe(listener)
            Client.connect(mutableListOf(Filter(
                authors = listOf(persona.publicKey.toHex()))))

//            val metaData = ContactMetaData("NostrPostr Testr", "http://www.makolkin.ru/Gallery/120930/Moon_120930_TAL-250K_IR_VAC136_dvmak001.jpg", "Just an account to play with NOSTR", null)
//            val event = MetadataEvent.create(metaData, persona.privateKey!!)
//            Client.send(event)

//            val fiatjaf = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
//            val monlovesmango = "8c0da4862130283ff9e67d889df264177a508974e2feb96de139804ea66d6168"
//            val leo = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
//            val richPeopleEvent = "184240587ebb618520a6b9be77484db0edf94b410db31933616bc4874739bca0"
//            val richPeopleReplyEvent = "354304c7d22fb3889f89d715bb40207b653ed5bd458802eef21f6431c3fb0333"
//            val msg = """A TextNoteEvent. The first published using [NostrPostr](https://github.com/Giszmo/NostrPostr):
//                |
//                |```
//                |val richPeopleReplyEvent = "354304c7d22fb3889f89d715bb40207b653ed5bd458802eef21f6431c3fb0333"
//                |val richPeopleEvent = "184240587ebb618520a6b9be77484db0edf94b410db31933616bc4874739bca0"
//                |val fiatjaf = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
//                |val monlovesmango = "8c0da4862130283ff9e67d889df264177a508974e2feb96de139804ea66d6168"
//                |val leo = "46fcbe3065eaf1ae7811465924e48923363ff3f526bd6f73d7c184b16bd8ce4d"
//                |Client.send(
//                |    TextNoteEvent.create(
//                |        msg,
//                |        replyTos = listOf(richPeopleReplyEvent, richPeopleEvent),
//                |        mentions = listOf(fiatjaf, monlovesmango, leo),
//                |        persona.privateKey!!
//                |    )
//                |)
//                |```
//                |
//                |... and sorry for the spam ;)
//            """.trimMargin()
//            val textNoteEvent = TextNoteEvent.create(msg, replyTos = listOf(richPeopleReplyEvent, richPeopleEvent), mentions = listOf(fiatjaf, monlovesmango, leo), persona.privateKey!!)
//            Client.send(textNoteEvent)

//            val encryptedDmEvent = EncryptedDmEvent.create(Hex.decode(pubKeyLeo), null, "Test 4", persona.privateKey!!)
//            println(encryptedDmEvent.toJson())
//            Client.send(encryptedDmEvent)
            while (running) {
                Thread.sleep(100)
            }
        }

        var running = true
        private fun stop() {
            Thread.sleep(2000)
            running = false
            Client.unsubscribe(listener)
            Client.disconnect()
        }
    }
}
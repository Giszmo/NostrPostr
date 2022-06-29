package nostr.postr.examples

import tornadofx.*

class MyApp: App(TornadoTest::class)

class TornadoTest : View("My View") {
    override val root = borderpane {
        label("Hello world")
    }
}

package nostr.postr

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import nostr.postr.databinding.ActivityMainBinding
import nostr.postr.events.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val clientListener = object : Client.Listener() {
        override fun onNewEvent(event: Event) {
            when (event.kind) {
                MetadataEvent.kind, // 0
                TextNoteEvent.kind, // 1
                RecommendRelayEvent.kind, // 2
                ContactListEvent.kind, // 3
                EncryptedDmEvent.kind, // 4
                DeletionEvent.kind, // 5
                in listOf(6, 7, 17, 30, 40, 7357) -> Unit
                else -> Log.d("UNHANDLED_EVENT", event.toJson())
            }
        }

        override fun onError(error: Error, relay: Relay) {
            Log.e("ERROR", "Relay ${relay.url}: ${error.message}")
        }

        override fun onRelayStateChange(type: Relay.Type, relay: Relay) {
            Log.d("RELAY", "Relay ${relay.url} ${when (type) {
                Relay.Type.CONNECT -> "connected."
                Relay.Type.DISCONNECT -> "disconnected."
                Relay.Type.EOSE -> "sent all events it had stored."
            }}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Client.subscribe(clientListener)
        Client.lenient = true
        val filter = Filter(
            since = Date(1652305000)
        )
        Client.connect(mutableListOf(filter))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_feed, R.id.navigation_notifications, R.id.navigation_messages, R.id.navigation_search
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}
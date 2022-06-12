package nostr.postr

import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import nostr.postr.databinding.ActivityMainBinding
import nostr.postr.events.*

class MainActivity : AppCompatActivity(), Client.Listener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Client.subscribe(this)
        Client.connect()

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

    override fun onNewEvent(event: Event) {
        when (event) {
            is MetadataEvent,
            is TextNoteEvent,
            is RecommendRelayEvent,
            is ContactListEvent,
            is EncryptedDmEvent -> Unit
            is DeletionEvent -> Log.d("DEL_EVENT", event.toJson())
            else ->
                if (event.kind !in listOf(6, 7, 17, 30, 7357)) Log.d("UNHANDLED_EVENT", event.toJson())
        }
    }

    override fun onEvent(event: Event, relay: Relay) {
    }

    override fun onError(error: Error, relay: Relay) {
        Log.e("ERROR", "Relay ${relay.url}: ${error.message}")
    }

    override fun onRelayStateChange(type: Int, relay: Relay) {
        Log.d("RELAY", "Relay ${relay.url} ${if (type == 0) "connected" else "disconnected"}")
    }
}
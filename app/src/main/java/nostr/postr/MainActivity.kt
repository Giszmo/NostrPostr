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

class MainActivity : AppCompatActivity(), Client.Listener {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Client.register(this)
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
        Log.d("NEW_MSG", "${event.pubkey} wrote kind ${event.kind}: ${event.content}")
    }

    override fun onEvent(event: Event, relay: Relay) {
        Log.d("EVENT", event.toJson())
        // Log.d("MSG", "$relay sent $msg")
    }

    override fun onError(error: Error, relay: Relay) {
        Log.e("ERROR", "Relay ${relay.url}: ${error.message}")
    }

    override fun onRelayStateChange(type: Int, relay: Relay) {
        Log.d("RELAY", "Relay ${relay.url} ${if (type == 0) "connected" else "disconnected"}")
    }
}
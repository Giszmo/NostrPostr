package nostr.postr.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.acinq.secp256k1.Hex
import nostr.postr.Persona
import nostr.postr.Utils

class HomeViewModel : ViewModel() {
    private val _personas: MutableLiveData<MutableList<Persona>> by lazy {
        MutableLiveData<MutableList<Persona>>().also {
            it.value = mutableListOf(
                Persona(privKey = Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).apply { petName = "Alice" },
                Persona(privKey = Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")).apply { petName = "Bob" },
                Persona(privKey = Hex.decode("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")).apply { petName = "Carol" },
                Persona(pubKey = Utils.pubkeyCreate(Hex.decode("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"))).apply { petName = "Dave" }
            )
            loadPersonas()
        }
    }
    val personas: LiveData<MutableList<Persona>> = _personas

    private fun loadPersonas() {
//        _personas.value..postValue(mutableListOf(
//            Persona(privateKey = Hex.decode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")).apply { petName = "Alice" },
//            Persona(privateKey = Hex.decode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")).apply { petName = "Bob" },
//            Persona(privateKey = Hex.decode("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc")).apply { petName = "Carol" },
//            Persona(publicKey = Utils.pubkeyCreate(Hex.decode("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"))).apply { petName = "Dave" }
//        ))
    }
}
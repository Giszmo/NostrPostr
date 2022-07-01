package nostr.postr.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import fr.acinq.secp256k1.Hex
import nostr.postr.Persona
import nostr.postr.PersonasRecyclerViewAdapter
import nostr.postr.Utils
import nostr.postr.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val rvPersonas = binding.rvPersonas
        rvPersonas.layoutManager = LinearLayoutManager(context)
        homeViewModel.personas.observe(viewLifecycleOwner) {
            rvPersonas.adapter = PersonasRecyclerViewAdapter(requireContext(), it)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

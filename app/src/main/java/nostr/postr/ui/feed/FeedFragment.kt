package nostr.postr.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import nostr.postr.FeedItem
import nostr.postr.FeedListAdapter
import nostr.postr.R
import nostr.postr.databinding.FragmentFeedBinding

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val feedViewModel =
            ViewModelProvider(this)[FeedViewModel::class.java]

        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.rvFeed.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this.context)
            adapter = FeedListAdapter(arrayOf(
                FeedItem(android.R.drawable.ic_dialog_email, "bla1"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla2"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla3"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla4"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla5"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla6"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla7"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla8"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla9"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla10"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla11"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla12"),
                FeedItem(android.R.drawable.ic_dialog_email, "bla13")
            ))
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
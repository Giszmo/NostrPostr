package nostr.postr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView


data class FeedItem(val imgId: Int, val description: String)

class FeedListAdapter(val listData: Array<FeedItem>) :
    RecyclerView.Adapter<FeedListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem: View = layoutInflater.inflate(R.layout.fragment_feed_item, parent, false)
        return ViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val myListData: FeedItem = listData[position]
        holder.textView.text = listData[position].description
        holder.imageView.setImageResource(listData[position].imgId)
        holder.relativeLayout.setOnClickListener {
            Toast.makeText(
                holder.relativeLayout.context,
                "click on item: " + myListData.description,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun getItemCount() = listData.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView
        var textView: TextView
        var relativeLayout: RelativeLayout

        init {
            imageView = itemView.findViewById(R.id.imageView) as ImageView
            textView = itemView.findViewById(R.id.textView)
            relativeLayout = itemView.findViewById(R.id.relativeLayout)
        }
    }
}
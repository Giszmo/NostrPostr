package nostr.postr

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PersonasRecyclerViewAdapter(context: Context, val data: MutableList<Persona>): RecyclerView.Adapter<PersonasRecyclerViewAdapter.ViewHolder>() {
    val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(R.layout.fragment_persona, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val persona = data[position]
        holder.petName.text = persona.petName ?: "Unknown"
        holder.pubKey.text = persona.pubKey.toHex().substring(0, 8).plus("…")
        holder.hasPrivKey.visibility = if (persona.privKey != null) View.VISIBLE else View.INVISIBLE
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(data: List<Persona>) {
        this.data.clear()
        notifyDataSetChanged()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val petName: TextView = itemView.findViewById(R.id.tvPetName)
        val pubKey: TextView = itemView.findViewById(R.id.tvPubKey)
        val hasPrivKey: ImageView = itemView.findViewById(R.id.ivHasPrivKey)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
        }
    }
}
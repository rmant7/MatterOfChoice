import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.matterofchoice.MessageModel
import com.matterofchoices.R

class CustomAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<MessageModel>()

    fun submitList(newMessages: List<MessageModel>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged() // Or use DiffUtil for efficiency
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].role) {
            "user" -> VIEW_TYPE_USER
            "model" -> VIEW_TYPE_MODEL
            else -> throw IllegalArgumentException("Invalid role")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(inflater.inflate(R.layout.item_user_recycler, parent, false))
            VIEW_TYPE_MODEL -> ModelMessageViewHolder(inflater.inflate(R.layout.item_recycler, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> {
                holder.bind(messages[position])
                holder.itemView.setOnLongClickListener {
                    onClick?.invoke(messages[position])
                    true
                }
            }
            is ModelMessageViewHolder -> holder.bind(messages[position])
        }
    }
    var onClick: ((MessageModel) -> Unit)? = null

    override fun getItemCount(): Int = messages.size

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_MODEL = 2
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: MessageModel) {
            val textView:TextView = itemView.findViewById(R.id.userTextView)
            textView.text = message.message
        }




    }
    class ModelMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: MessageModel) {
            val textView:TextView = itemView.findViewById(R.id.textView3)
            val situationNumber = (position % 4) + 1 // Cycles through 1 to 4
            textView.text = "Situation $situationNumber: ${message.message}"
        }
    }


}

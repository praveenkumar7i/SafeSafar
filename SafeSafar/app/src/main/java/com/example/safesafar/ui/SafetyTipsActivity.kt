package com.example.safesafar.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safesafar.R

data class Tip(val header: String, val content: String, var isExpanded: Boolean = false)

class SafetyTipsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safety_tips)

        val tips = listOf(
            Tip(
                "1. General Safety Tips",
                """• Be aware of your surroundings at all times.
• Always trust your instincts—if it feels wrong, leave.
• Have an escape plan when visiting new places.
• Keep your emergency contacts updated and ready.
• Avoid distractions like being glued to your phone while walking.""".trimIndent()
            ),
            Tip(
                "2. Travel Safety",
                """• Always verify driver and cab details before boarding.
• Share your live route and vehicle details with a trusted contact.
• Sit in the back seat to maintain a safe distance.
• Ensure child locks are disabled and emergency exits are accessible.
• Avoid oversharing personal information with strangers or drivers.""".trimIndent()
            ),
            Tip(
                "3. Night Safety",
                """• Avoid taking isolated or poorly lit routes.
• Stay alert and keep your phone charged and accessible.
• Carry emergency tools like a whistle or pepper spray.
• Walk confidently and purposefully to deter potential threats.
• Stick to well-lit areas with more foot traffic.""".trimIndent()
            ),
            Tip(
                "4. Digital Safety",
                """• Protect your privacy and don't overshare online.
• Use strong, unique passwords for all your accounts.
• Avoid clicking on unknown or suspicious links.
• Immediately report and block abusive profiles or messages.
• Control and review who has access to your location sharing.""".trimIndent()
            ),
            Tip(
                "5. Indian Constitutional Laws",
                """• IPC 354: Assault or criminal force to woman with intent to outrage her modesty.
• IPC 354D: Stalking.
• IPC 509: Word, gesture or act intended to insult the modesty of a woman.
• Zero FIR: You can file an FIR at ANY police station regardless of jurisdiction.
• Right to free legal aid for women.
• Emergency Helplines: Police (112), Women Helpline (1091).""".trimIndent()
            )
        )

        val rv = findViewById<RecyclerView>(R.id.rvTips)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = TipAdapter(tips)
    }
}

class TipAdapter(private val tips: List<Tip>) :
    RecyclerView.Adapter<TipAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tvHeader)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expandable_tip, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tip = tips[position]

        holder.tvHeader.text = tip.header
        holder.tvContent.text = tip.content
        holder.tvContent.visibility = if (tip.isExpanded) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            tip.isExpanded = !tip.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = tips.size
}
package com.example.safesafar.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayout
import com.example.safesafar.R

data class Video(val title: String, val duration: String, val videoId: String, val isShort: Boolean = false)

class SelfDefenseActivity : AppCompatActivity() {

    // ── Full Videos ──────────────────────────────────────────────────────────
    private val fullVideos = listOf(
        Video("Basic Self Defense Moves",        "3:45", "KVpxP3ZZtAc"),
        Video("Women Self Defense Techniques",   "5:20", "MybIOuS30Ik"),
        Video("How to Escape a Grab",            "2:30", "9m-x64bKfR4"),
        Video("Defense Against Chokes",          "4:15", "jeDFQFdi3Vc"),
        Video("Street Self Defense Tips",        "6:10", "M4_8PoRQP8w")
    )

    // ── Shorts ───────────────────────────────────────────────────────────────
    private val shorts = listOf(
        Video("Quick Escape Move",  "0:30", "EDCr-3D6jQ0", true),
        Video("Wrist Grab Defense", "0:30", "N7-A0RsCGbo", true),
        Video("Stop an Attacker",   "0:30", "yMRdmQ6VWk0", true),
        Video("Choke Escape",       "0:30", "WGVvw7DdlPY", true),
        Video("Fast Self Defense",  "0:30", "AgPEFhPuOTE", true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_self_defense)
        supportActionBar?.title = "Self Defense Training"

        val rvVideos  = findViewById<RecyclerView>(R.id.rvVideos)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        rvVideos.layoutManager = LinearLayoutManager(this)

        fun openPlayer(video: Video) {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.videoId}"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}"))
            try {
                startActivity(appIntent)
            } catch (ex: ActivityNotFoundException) {
                startActivity(webIntent)
            }
        }

        rvVideos.adapter = VideoAdapter(fullVideos) { openPlayer(it) }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                rvVideos.adapter = when (tab?.position) {
                    1    -> VideoAdapter(shorts)     { openPlayer(it) }
                    else -> VideoAdapter(fullVideos) { openPlayer(it) }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}

// ── Adapter ──────────────────────────────────────────────────────────────────

class VideoAdapter(
    private val videos: List<Video>,
    private val onClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle:     TextView  = view.findViewById(R.id.tvVideoTitle)
        val tvDuration:  TextView  = view.findViewById(R.id.tvVideoDuration)
        val btnPlay:     Button    = view.findViewById(R.id.btnPlayVideo)
        val imgThumbnail: ImageView = view.findViewById(R.id.imgThumbnail)
    }

    override fun getItemViewType(position: Int): Int {
        return if (videos[position].isShort) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (viewType == 1) R.layout.item_short else R.layout.item_video
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        holder.tvTitle.text    = video.title
        holder.tvDuration.text = video.duration

        // ── Load YouTube thumbnail via Glide ──────────
        val thumbnailUrl = "https://img.youtube.com/vi/${video.videoId}/0.jpg"
        Glide.with(holder.itemView.context)
            .load(thumbnailUrl)
            .apply(
                RequestOptions()
                    .transform(RoundedCorners(24))
                    .placeholder(R.drawable.rounded_thumbnail_bg)
                    .error(R.drawable.rounded_thumbnail_bg)
            )
            .into(holder.imgThumbnail)

        // ── Click animation + open player ──────────────────────────────────
        val clickAction = {
            holder.itemView.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(80)
                .withEndAction {
                    holder.itemView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    onClick(video)
                }.start()
        }

        holder.btnPlay.setOnClickListener    { clickAction() }
        holder.itemView.setOnClickListener   { clickAction() }
    }

    override fun getItemCount() = videos.size
}

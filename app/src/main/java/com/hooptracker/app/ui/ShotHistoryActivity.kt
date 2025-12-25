package com.hooptracker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hooptracker.app.HoopTrackerApplication
import com.hooptracker.app.R
import com.hooptracker.app.data.Shot
import com.hooptracker.app.databinding.ActivityShotHistoryBinding
import com.hooptracker.app.databinding.ItemShotBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ShotHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShotHistoryBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as HoopTrackerApplication).repository)
    }
    private val adapter = ShotHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShotHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shot History"

        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val shot = adapter.getItem(position)
                viewModel.deleteShot(shot.id)

                Snackbar.make(binding.root, "Shot deleted", Snackbar.LENGTH_SHORT).show()
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun observeData() {
        viewModel.allShots.observe(this) { shots ->
            adapter.submitList(shots)
            binding.emptyText.visibility = if (shots.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class ShotHistoryAdapter : RecyclerView.Adapter<ShotHistoryAdapter.ShotViewHolder>() {
        private var shots = listOf<Shot>()
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun submitList(newShots: List<Shot>) {
            shots = newShots
            notifyDataSetChanged()
        }

        fun getItem(position: Int): Shot = shots[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShotViewHolder {
            val binding = ItemShotBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ShotViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ShotViewHolder, position: Int) {
            holder.bind(shots[position], dateFormat)
        }

        override fun getItemCount(): Int = shots.size

        class ShotViewHolder(private val binding: ItemShotBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(shot: Shot, dateFormat: SimpleDateFormat) {
                binding.tvResult.text = if (shot.isHit) "✓ HIT" else "✗ MISS"
                binding.tvResult.setTextColor(
                    if (shot.isHit)
                        binding.root.context.getColor(R.color.success)
                    else
                        binding.root.context.getColor(R.color.error)
                )
                binding.tvTimestamp.text = dateFormat.format(shot.getDate())
                binding.tvShotType.text = shot.getShotType().name.replace("_", " ")
            }
        }
    }
}

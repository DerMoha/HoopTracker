package com.hooptracker.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.hooptracker.app.HoopTrackerApplication
import com.hooptracker.app.R
import com.hooptracker.app.data.Shot
import com.hooptracker.app.data.ShotType
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

        binding.toolbar.setNavigationOnClickListener { finish() }
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
                val shot = adapter.getItemAt(position)
                viewModel.deleteShot(shot.id)

                Snackbar.make(binding.root, R.string.shot_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        viewModel.restoreShot(shot)
                        Snackbar.make(binding.root, R.string.shot_restored, Snackbar.LENGTH_SHORT).show()
                    }
                    .show()
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private fun observeData() {
        viewModel.allShots.observe(this) { shots ->
            adapter.submitList(shots)
            binding.emptyState.visibility = if (shots.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class ShotHistoryAdapter : ListAdapter<Shot, ShotHistoryAdapter.ShotViewHolder>(ShotDiffCallback()) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun getItemAt(position: Int): Shot = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShotViewHolder {
            val binding = ItemShotBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ShotViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ShotViewHolder, position: Int) {
            holder.bind(getItem(position), dateFormat)
        }

        class ShotViewHolder(private val binding: ItemShotBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(shot: Shot, dateFormat: SimpleDateFormat) {
                binding.tvResult.text = binding.root.context.getString(
                    if (shot.isHit) R.string.made_result else R.string.miss_result
                )
                binding.tvResult.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(if (shot.isHit) R.color.success else R.color.error)
                )
                binding.tvTimestamp.text = dateFormat.format(shot.getDate())
                binding.tvShotType.text = formatShotType(shot.getShotTypeEnum())
            }

            private fun formatShotType(shotType: ShotType): String = when (shotType) {
                ShotType.GENERAL -> binding.root.context.getString(R.string.general_shot_type)
                ShotType.THREE_POINTER -> binding.root.context.getString(R.string.three_point_shot_type)
                ShotType.MID_RANGE -> binding.root.context.getString(R.string.mid_range_shot_type)
                ShotType.LAYUP -> binding.root.context.getString(R.string.layup_shot_type)
                ShotType.FREE_THROW -> binding.root.context.getString(R.string.free_throw_shot_type)
            }
        }

        class ShotDiffCallback : DiffUtil.ItemCallback<Shot>() {
            override fun areItemsTheSame(oldItem: Shot, newItem: Shot): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Shot, newItem: Shot): Boolean = oldItem == newItem
        }
    }
}

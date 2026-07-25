package com.rhdevs.rhpatch.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wmods.wppenhacer.R
import com.rhdevs.rhpatch.scheduler.db.UniversalTaskEntity
import com.wmods.wppenhacer.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.ImageView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip

class TasksFragment : Fragment() {
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var chipGroupFilter: ChipGroup
    private lateinit var adapter: TasksAdapter
    private var allTasks: List<UniversalTaskEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        rvTasks = view.findViewById(R.id.rv_tasks)
        tvEmpty = view.findViewById(R.id.tv_empty)
        chipGroupFilter = view.findViewById(R.id.chip_group_filter)

        rvTasks.layoutManager = LinearLayoutManager(context)
        adapter = TasksAdapter()
        rvTasks.adapter = adapter
        
        chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            filterTasks()
        }

        adapter.onItemClick = { task ->
            val intent = android.content.Intent(requireContext(), com.rhdevs.rhpatch.activity.ComposeScheduleActivity::class.java).apply {
                putExtra("edit_task_id", task.id)
                putExtra("edit_target_app", task.targetApp)
                putExtra("edit_recipient", task.recipientPhoneOrEmail)
                putExtra("edit_message", task.message)
                putExtra("edit_time", task.triggerTimeMillis)
            }
            startActivity(intent)
        }
        
        adapter.onDeleteClick = { task ->
            val context = requireContext()
            val db = AppDatabase.getInstance(context).universalSchedulerDao()
            db.deleteTask(task)
            loadTasks() // reload data
        }

        loadTasks()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        val context = requireContext()
        val db = AppDatabase.getInstance(context).universalSchedulerDao()
        allTasks = db.getAllTasks()
        filterTasks()
    }
    
    private fun filterTasks() {
        if (!::chipGroupFilter.isInitialized) return
        val checkedId = chipGroupFilter.checkedChipId
        val filteredTasks = when (checkedId) {
            R.id.chip_pending -> allTasks.filter { it.status == "PENDING" || it.status == "PROCESSING" }
            R.id.chip_success -> allTasks.filter { it.status == "SUCCESS" }
            R.id.chip_failed -> allTasks.filter { it.status == "FAILED" }
            else -> allTasks
        }
        
        if (filteredTasks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvTasks.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvTasks.visibility = View.VISIBLE
            adapter.setTasks(filteredTasks)
        }
    }
}

class TasksAdapter : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {
    private var tasks = listOf<UniversalTaskEntity>()
    var onItemClick: (UniversalTaskEntity) -> Unit = {}
    var onDeleteClick: (UniversalTaskEntity) -> Unit = {}

    fun setTasks(newTasks: List<UniversalTaskEntity>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppInitial: TextView = view.findViewById(R.id.tv_app_initial)
        val tvAppName: TextView = view.findViewById(R.id.tv_app_name)
        val tvRecipient: TextView = view.findViewById(R.id.tv_recipient)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
        val tvStatus: TextView = view.findViewById(R.id.tv_status)
        val ivDelete: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_universal_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        
        holder.tvAppInitial.text = task.targetApp.firstOrNull()?.uppercase() ?: "?"
        holder.tvAppName.text = task.targetApp.replaceFirstChar { it.uppercase() }
        holder.tvRecipient.text = task.recipientPhoneOrEmail
        holder.tvMessage.text = task.message
        
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        holder.tvTime.text = sdf.format(task.triggerTimeMillis)
        
        holder.tvStatus.text = task.status
        when (task.status) {
            "SUCCESS" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#10B981"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#3310B981"))
            }
            "FAILED" -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#33EF4444"))
            }
            else -> {
                holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                holder.tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#33F59E0B"))
            }
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(task)
        }
        
        holder.ivDelete.setOnClickListener {
            onDeleteClick(task)
        }
    }

    override fun getItemCount() = tasks.size

}

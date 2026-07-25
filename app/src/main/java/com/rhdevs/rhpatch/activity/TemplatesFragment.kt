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
import com.rhdevs.rhpatch.scheduler.db.UniversalTemplateEntity
import com.wmods.wppenhacer.database.AppDatabase

class TemplatesFragment : Fragment() {
    private lateinit var rvList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: TemplatesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_generic_list, container, false)
        rvList = view.findViewById(R.id.rv_list)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tvEmpty.text = "Belum ada templat tersimpan."

        rvList.layoutManager = LinearLayoutManager(context)
        adapter = TemplatesAdapter()
        rvList.adapter = adapter

        loadTemplates()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadTemplates()
    }

    private fun loadTemplates() {
        val db = AppDatabase.getInstance(requireContext()).universalSchedulerDao()
        val list = db.getAllTemplates()
        if (list.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvList.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvList.visibility = View.VISIBLE
            adapter.setItems(list)
        }
    }
}

class TemplatesAdapter : RecyclerView.Adapter<TemplatesAdapter.ViewHolder>() {
    private var items = listOf<UniversalTemplateEntity>()

    fun setItems(newItems: List<UniversalTemplateEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_generic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.message
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)
    }
}

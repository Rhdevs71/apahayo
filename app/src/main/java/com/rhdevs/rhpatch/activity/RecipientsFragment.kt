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
import com.rhdevs.rhpatch.scheduler.db.UniversalRecipientEntity
import com.wmods.wppenhacer.database.AppDatabase

class RecipientsFragment : Fragment() {
    private lateinit var rvList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: RecipientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_generic_list, container, false)
        rvList = view.findViewById(R.id.rv_list)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tvEmpty.text = "Belum ada penerima tersimpan."

        rvList.layoutManager = LinearLayoutManager(context)
        adapter = RecipientsAdapter()
        rvList.adapter = adapter

        loadRecipients()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadRecipients()
    }

    private fun loadRecipients() {
        val db = AppDatabase.getInstance(requireContext()).universalSchedulerDao()
        val list = db.getAllRecipients()
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

class RecipientsAdapter : RecyclerView.Adapter<RecipientsAdapter.ViewHolder>() {
    private var items = listOf<UniversalRecipientEntity>()

    fun setItems(newItems: List<UniversalRecipientEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_generic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.name
        holder.tvSubtitle.text = "${item.phoneOrEmail} (${item.groupName})"
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)
    }
}

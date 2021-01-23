package io.github.zarandya.beatrate.adapter

import android.util.Log.d
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.RenderProcessGoneDetail
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.poupa.vinylmusicplayer.util.PreferenceUtil
import com.poupa.vinylmusicplayer.util.ViewUtil
import io.github.zarandya.beatrate.R
import io.github.zarandya.beatrate.ui.TargetRateSettingsDialog

class TargetRateSettingsAdapter(
        private val activity: AppCompatActivity
): RecyclerView.Adapter<TargetRateSettingsViewHolder>(), DraggableItemAdapter<TargetRateSettingsViewHolder> {
    
    var data: ArrayList<Double> = PreferenceUtil.getInstance().targetRates
    
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TargetRateSettingsViewHolder {
        val view = LayoutInflater.from(activity).inflate(R.layout.target_rate_settings_item_list, parent, false)

        val holder = TargetRateSettingsViewHolder(view)
        holder.setOnEditClickListener {
            val position = if (holder.adapterPosition < data.size) holder.adapterPosition else -1
            val value = if (position >= 0) data[position] else 20.0
            val dialog = TargetRateSettingsDialog(position, value) {
                loadModifiedData()
            }
            dialog.show(activity.supportFragmentManager, "EDIT_TARGET_RATE")
        }
        holder.delete?.setOnClickListener {
            data.removeAt(holder.adapterPosition)
            saveModifiedData()
            notifyDataSetChanged()
        }

        return holder
    }

    override fun getItemCount(): Int = data.size + 1

    override fun onBindViewHolder(holder: TargetRateSettingsViewHolder, position: Int) {
        if (position < data.size) {
            holder.dragView?.visibility = VISIBLE
            holder.title?.visibility = VISIBLE
            holder.delete?.visibility = VISIBLE
            holder.edit?.setImageResource(R.drawable.ic_settings_white_24dp)
            val layout = holder.edit?.layoutParams
            layout?.width = WRAP_CONTENT
            holder.edit?.layoutParams = layout
            holder.title?.text = data[position].toString()
        }
        else {
            // this is the plus button
            holder.dragView?.visibility = GONE
            holder.title?.visibility = GONE
            holder.delete?.visibility = GONE
            holder.edit?.setImageResource(R.drawable.ic_library_add_white_24dp) // should have generic add icon
            val layout = holder.edit?.layoutParams
            layout?.width = MATCH_PARENT
            holder.edit?.layoutParams = layout
        }
    }

    override fun onGetItemDraggableRange(p0: TargetRateSettingsViewHolder?, p1: Int): ItemDraggableRange {
        d("TargetRateSettingsAdapt", "onGetItemDraggableRange")
        return ItemDraggableRange(0, data.size - 1)
    }

    override fun onCheckCanStartDrag(holder: TargetRateSettingsViewHolder?, position: Int, x: Int, y: Int): Boolean {
        d("TargetRateSettingsAdapt", "onCheckCanStartDrag ${holder?.dragView} $x, $y ${ViewUtil.hitTest(holder?.dragView, x, y)} $position")
        return position < data.size && holder != null && ViewUtil.hitTest(holder.dragView, x, y)
    }

    override fun onItemDragStarted(position: Int) {
        d("TargetRateSettingsAdapt", "onItemDragStarted")
        notifyDataSetChanged() // why?
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        d("TargetRateSettingsAdapt", "onMoveItem")
        data.add(toPosition, data.removeAt(fromPosition));
        itemIds.add(toPosition, itemIds.removeAt(fromPosition));
        saveModifiedData()
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        d("TargetRateSettingsAdapt", "onCheckCanDrop")
        return dropPosition < data.size
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        d("TargetRateSettingsAdapt", "onItemDragFinished")
        notifyDataSetChanged()
    }
    
    private fun saveModifiedData() {
        super.notifyDataSetChanged()
        PreferenceUtil.getInstance().targetRates = data;
    }
    
    private fun loadModifiedData() {
        data = PreferenceUtil.getInstance().targetRates
        notifyDataSetChanged()
    }

    private val itemIds = ArrayList<Long>()
    private var maxItemId = 0L
    
    override fun getItemId(position: Int): Long {
        while (itemIds.size <= position) {
            itemIds += (maxItemId++)
        }
        return itemIds[position]
    }
}
package io.github.zarandya.beatrate.adapter

import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import butterknife.BindView
import butterknife.ButterKnife
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder
import com.poupa.vinylmusicplayer.views.TouchInterceptHorizontalScrollView
import io.github.zarandya.beatrate.R

class TargetRateSettingsViewHolder(itemView: View) : AbstractDraggableSwipeableItemViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {

    @DraggableItemStateFlags private var mDragStateFlags = 0

    override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
        mDragStateFlags = flags
    }

    @DraggableItemStateFlags
    override fun getDragStateFlags(): Int {
        return mDragStateFlags
    }

    /*@BindView(R.id.song_view)
    var songView: LinearLayout? = null

    @BindView(R.id.touch_intercept_framelayout)
    var dummyContainer: FrameLayout? = null*/

    @JvmField
    @BindView(R.id.title)
    var title: TextView? = null

    /*@BindView(R.id.title_scrollview)
    var titleScrollview: TouchInterceptHorizontalScrollView? = null*/

    @JvmField
    @BindView(R.id.text)
    var text: TextView? = null

    @JvmField
    @BindView(R.id.edit)
    var edit: AppCompatImageView? = null

    @JvmField
    @BindView(R.id.delete)
    var delete: View? = null

    @JvmField
    @BindView(R.id.separator)
    var separator: View? = null

    @JvmField
    @BindView(R.id.short_separator)
    var shortSeparator: View? = null

    @JvmField
    @BindView(R.id.drag_view)
    var dragView: View? = null

    /*@BindView(R.id.palette_color_container)
    var paletteColorContainer: View? = null*/

    init {
        ButterKnife.bind(this, itemView)
        itemView.setOnClickListener(this)
        edit?.setOnClickListener {  }
    }
    
    fun setOnEditClickListener(l: ((View) -> Unit)) {
        edit?.setOnClickListener(l);
    }
    
    fun setOnDeleteClickListener(l: ((View) -> Unit)) {
        delete?.setOnClickListener(l);
    }

    override fun getSwipeableContainerView(): View? {
        return null
    }

    override fun onClick(v: View?) {}
    override fun onLongClick(v: View?): Boolean = false
}
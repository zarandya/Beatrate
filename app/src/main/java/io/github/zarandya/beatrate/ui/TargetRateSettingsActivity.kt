package io.github.zarandya.beatrate.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.h6ah4i.android.widget.advrecyclerview.animator.RefactoredDefaultItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils
import com.kabouzeid.appthemehelper.ThemeStore
import com.kabouzeid.appthemehelper.util.ColorUtil
import com.kabouzeid.appthemehelper.util.MaterialValueHelper
import com.poupa.vinylmusicplayer.ui.activities.base.AbsBaseActivity
import com.poupa.vinylmusicplayer.util.ViewUtil
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import io.github.zarandya.beatrate.R
import io.github.zarandya.beatrate.adapter.TargetRateSettingsAdapter

class TargetRateSettingsActivity: AbsBaseActivity() {
    
    @JvmField @BindView(R.id.recycler_view) var recyclerView: FastScrollRecyclerView? = null
    @JvmField @BindView(R.id.toolbar) var toolbar: Toolbar? = null
    @JvmField @BindView(R.id.title) var titleTextView: TextView? = null
    
    private var recyclerViewDragDropManager: RecyclerViewDragDropManager? = null
    private var wrappedAdapter: RecyclerView.Adapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDrawUnderStatusbar()
        setContentView(R.layout.activity_beatrate_target_rate_settings)
        ButterKnife.bind(this);

        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setTaskDescriptionColorAuto()

        ViewUtil.setUpFastScrollRecyclerViewColor(this, recyclerView, ThemeStore.accentColor(this))
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        
        val adapter = TargetRateSettingsAdapter(this)
        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        wrappedAdapter = recyclerViewDragDropManager!!.createWrappedAdapter(adapter)
        recyclerView!!.adapter = wrappedAdapter
        recyclerView!!.itemAnimator = RefactoredDefaultItemAnimator()
        recyclerViewDragDropManager!!.attachRecyclerView(recyclerView!!)
        recyclerViewDragDropManager!!.setInitiateOnTouch(true)

        setUpToolbar()
    }

    private fun setUpToolbar() {
        toolbar!!.setBackgroundColor(ThemeStore.primaryColor(this))
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = null
        titleTextView!!.text = getString(R.string.beatrate_target_rate_settings_activity_title)
        titleTextView!!.setTextColor(MaterialValueHelper.getPrimaryTextColor(this, ColorUtil.isColorLight(ThemeStore.primaryColor(this))))
    }

    override fun onPause() {
        recyclerViewDragDropManager?.cancelDrag()
        super.onPause()
    }

    override fun onDestroy() {
        recyclerViewDragDropManager?.release()
        recyclerViewDragDropManager = null

        recyclerView!!.itemAnimator = null
        recyclerView!!.adapter = null

        WrapperAdapterUtils.releaseAll(wrappedAdapter)
        wrappedAdapter = null

        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
package io.github.zarandya.beatrate.ui

import io.github.zarandya.beatrate.R

import android.R.string.ok
import android.R.string.cancel
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.MaterialDialog.InputCallback
import com.poupa.vinylmusicplayer.dialogs.RenamePlaylistDialog
import com.poupa.vinylmusicplayer.util.PlaylistsUtil
import com.poupa.vinylmusicplayer.util.PreferenceUtil

private const val TARGET_BEAT_ITEM_ID = "id"
private const val TARGET_BEAT_ITEM_VALUE = "value"

class TargetRateSettingsDialog(val position: Int, val value: Double, val callback: (() -> Unit)? = null): DialogFragment() {

    /*init {
        arguments = Bundle().apply {
            putInt(TARGET_BEAT_ITEM_ID, position)
            putDouble(TARGET_BEAT_ITEM_VALUE, value)
        }
    }*/

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //val args = requireArguments()
        //val position = args.getInt(TARGET_BEAT_ITEM_ID)
        //val value = args.getDouble(TARGET_BEAT_ITEM_VALUE)
        return MaterialDialog.Builder(requireActivity())
                .title(getString(R.string.beatrate_change_target_rate_item_value_title))
                .positiveText(ok)
                .negativeText(cancel)
                .inputType(TYPE_CLASS_NUMBER or TYPE_NUMBER_FLAG_DECIMAL)
                .input(getString(R.string.beatrate_target_rate_empty), value.toString(), false,
                        InputCallback { _: MaterialDialog?, charSequence: CharSequence ->
                            val name = charSequence.toString().toDoubleOrNull()
                            if (name != null && name > 0) {
                                val array = PreferenceUtil.getInstance().targetRates
                                if (position >= 0) 
                                    array[position] = name
                                else
                                    array += name
                                PreferenceUtil.getInstance().targetRates = array
                                callback?.invoke();
                            }
                        })
                .build()
    }
}
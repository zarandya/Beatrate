package com.poupa.vinylmusicplayer;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.poupa.vinylmusicplayer.appshortcuts.DynamicShortcutManager;
import io.github.zarandya.beatrate.R;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class App extends MultiDexApplication {
    public static final String TAG = App.class.getSimpleName();

    private static App app;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        context = getApplicationContext();

        Log.d("NATIVE_PATH", getApplicationInfo().nativeLibraryDir);
        // default theme
        if (!ThemeStore.isConfigured(this, 1)) {
            ThemeStore.editTheme(this)
                    .primaryColorRes(R.color.md_indigo_500)
                    .accentColorRes(R.color.md_pink_A400)
                    .commit();
        }

        // Set up dynamic shortcuts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).initDynamicShortcuts();
        }
    }

    public static App getInstance() {
        return app;
    }

    public static Context getStaticContext() {
        return context;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}

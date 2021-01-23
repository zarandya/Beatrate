package com.poupa.vinylmusicplayer.appwidgets;

import android.widget.RemoteViews;

import io.github.zarandya.beatrate.R;
import com.poupa.vinylmusicplayer.appwidgets.base.BaseAppWidget;
import com.poupa.vinylmusicplayer.service.MusicService;

public class AppWidgetClassic extends BaseAppWidget {
    public static final String NAME = "app_widget_classic";

    private static AppWidgetClassic mInstance;

    public static synchronized AppWidgetClassic getInstance() {
        if (mInstance == null) {
            mInstance = new AppWidgetClassic();
        }
        return mInstance;
    }

    /**
     * Update all active widget instances by pushing changes
     */
    public void performUpdate(final MusicService service, final int[] appWidgetIds) {
        appWidgetView = new RemoteViews(service.getPackageName(), getLayout());

        // Set the titles and artwork
        setTitlesArtwork(service);

        // Link actions buttons to intents
        linkButtons(service);

        // Load the album cover async and push the update on completion
        loadAlbumCover(service, appWidgetIds);
    }

    public int getLayout() {
        return R.layout.app_widget_classic;
    }

    public int getId() {
        return R.id.app_widget_classic;
    }

    public int getImageSize(final MusicService service) {
        return service.getResources().getDimensionPixelSize(R.dimen.app_widget_classic_image_size);
    }

    public float getCardRadius(final MusicService service) {
        return service.getResources().getDimension(R.dimen.app_widget_card_radius);
    }
}

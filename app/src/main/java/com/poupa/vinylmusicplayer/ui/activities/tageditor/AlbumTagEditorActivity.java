package com.poupa.vinylmusicplayer.ui.activities.tageditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.Transition;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.kabouzeid.appthemehelper.util.ToolbarContentTintHelper;
import io.github.zarandya.beatrate.R;
import com.poupa.vinylmusicplayer.glide.GlideApp;
import com.poupa.vinylmusicplayer.glide.VinylSimpleTarget;
import com.poupa.vinylmusicplayer.glide.palette.BitmapPaletteWrapper;
import com.poupa.vinylmusicplayer.loader.AlbumLoader;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.util.ImageUtil;
import com.poupa.vinylmusicplayer.util.VinylMusicPlayerColorUtil;

import org.jaudiotagger.tag.FieldKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AlbumTagEditorActivity extends AbsTagEditorActivity implements TextWatcher {

    @BindView(R.id.title)
    EditText albumTitle;
    @BindView(R.id.artist)
    EditText artist;
    @BindView(R.id.album_artist)
    EditText albumArtist;
    @BindView(R.id.genre)
    EditText genre;
    @BindView(R.id.year)
    EditText year;

    private Bitmap albumArtBitmap;
    private boolean deleteAlbumArt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        setUpViews();
    }

    private void setUpViews() {
        fillViewsWithFileTags();
        albumTitle.addTextChangedListener(this);
        artist.addTextChangedListener(this);
        albumArtist.addTextChangedListener(this);
        genre.addTextChangedListener(this);
        year.addTextChangedListener(this);
    }


    private void fillViewsWithFileTags() {
        albumTitle.setText(getAlbumTitle());
        artist.setText(getArtistName());
        albumArtist.setText(getAlbumArtistName());
        genre.setText(getGenreName());
        year.setText(getSongYear());
    }

    @Override
    protected void loadCurrentImage() {
        Bitmap bitmap = getAlbumArt();
        setImageBitmap(bitmap, VinylMusicPlayerColorUtil.getColor(VinylMusicPlayerColorUtil.generatePalette(bitmap), ATHUtil.resolveColor(this, R.attr.defaultFooterColor)));
        deleteAlbumArt = false;
    }

        /*
    @Override
    protected void getImageFromLastFM() {
        String albumTitleStr = albumTitle.getText().toString();
        String albumArtistNameStr = albumArtist.getText().toString();
        if (albumArtistNameStr.trim().equals("") || albumTitleStr.trim().equals("")) {
            Toast.makeText(this, getResources().getString(R.string.album_or_artist_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        lastFMRestClient.getApiService().getAlbumInfo(albumTitleStr, albumArtistNameStr, null).enqueue(new Callback<LastFmAlbum>() {
            @Override
            public void onResponse(Call<LastFmAlbum> call, Response<LastFmAlbum> response) {
                LastFmAlbum lastFmAlbum = response.body();
                if (lastFmAlbum.getAlbum() != null) {
                    String url = LastFMUtil.getLargestAlbumImageUrl(lastFmAlbum.getAlbum().getImage());
                    if (!TextUtils.isEmpty(url) && url.trim().length() > 0) {
                        GlideApp.with(AlbumTagEditorActivity.this)
                                .as(BitmapPaletteWrapper.class)
                                .load(url)
                                .apply(new RequestOptions()
                                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                        .error(R.drawable.default_album_art))
                                .transition(new GenericTransitionOptions<BitmapPaletteWrapper>().transition(android.R.anim.fade_in))
                                .into(new VinylSimpleTarget<BitmapPaletteWrapper>() {
                                    @Override
                                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                        super.onLoadFailed(errorDrawable);
                                    }

                                    @Override
                                    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, Transition<? super BitmapPaletteWrapper> glideAnimation) {
                                        albumArtBitmap = ImageUtil.resizeBitmap(resource.getBitmap(), 2048);
                                        setImageBitmap(albumArtBitmap, VinylMusicPlayerColorUtil.getColor(resource.getPalette(), ATHUtil.resolveColor(AlbumTagEditorActivity.this, R.attr.defaultFooterColor)));
                                        deleteAlbumArt = false;
                                        dataChanged();
                                        setResult(RESULT_OK);
                                    }
                                });
                        return;
                    }
                }
                toastLoadingFailed();
            }

            @Override
            public void onFailure(Call<LastFmAlbum> call, Throwable t) {
                toastLoadingFailed();
            }

            private void toastLoadingFailed() {
                Toast.makeText(AlbumTagEditorActivity.this,
                        R.string.could_not_download_album_cover, Toast.LENGTH_SHORT).show();
            }
        });
    }
         */

    @Override
    protected void searchImageOnWeb() {
        searchWebFor(albumTitle.getText().toString(), albumArtist.getText().toString());
    }

    @Override
    protected void deleteImage() {
        setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art), ATHUtil.resolveColor(this, R.attr.defaultFooterColor));
        deleteAlbumArt = true;
        dataChanged();
    }

    @Override
    protected void save() {
        Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
        fieldKeyValueMap.put(FieldKey.ALBUM, albumTitle.getText().toString());
        //android seems not to recognize album_artist field so we additionally write the normal artist field
        fieldKeyValueMap.put(FieldKey.ARTIST, artist.getText().toString());
        fieldKeyValueMap.put(FieldKey.ALBUM_ARTIST, albumArtist.getText().toString());
        fieldKeyValueMap.put(FieldKey.GENRE, genre.getText().toString());
        fieldKeyValueMap.put(FieldKey.YEAR, year.getText().toString());

        writeValuesToFiles(fieldKeyValueMap, deleteAlbumArt ? new ArtworkInfo(getId(), null) : albumArtBitmap == null ? null : new ArtworkInfo(getId(), albumArtBitmap));
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.activity_album_tag_editor;
    }

    @NonNull
    @Override
    protected List<String> getSongPaths() {
        ArrayList<Song> songs = AlbumLoader.getAlbum(this, getId()).songs;
        ArrayList<String> paths = new ArrayList<>(songs.size());
        for (Song song : songs) {
            paths.add(song.data);
        }
        return paths;
    }

    @Override
    protected void loadImageFromFile(@NonNull final Uri selectedFileUri) {
        GlideApp.with(AlbumTagEditorActivity.this)
                .as(BitmapPaletteWrapper.class)
                .load(selectedFileUri)
                .transition(new GenericTransitionOptions<BitmapPaletteWrapper>().transition(android.R.anim.fade_in))
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true))
                .into(new VinylSimpleTarget<BitmapPaletteWrapper>() {
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                    }

                    @Override
                    public void onResourceReady(@NonNull BitmapPaletteWrapper resource, Transition<? super BitmapPaletteWrapper> glideAnimation) {
                        VinylMusicPlayerColorUtil.getColor(resource.getPalette(), Color.TRANSPARENT);
                        albumArtBitmap = ImageUtil.resizeBitmap(resource.getBitmap(), 2048);
                        setImageBitmap(albumArtBitmap, VinylMusicPlayerColorUtil.getColor(resource.getPalette(), ATHUtil.resolveColor(AlbumTagEditorActivity.this, R.attr.defaultFooterColor)));
                        deleteAlbumArt = false;
                        dataChanged();
                        setResult(RESULT_OK);
                    }
                });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        dataChanged();
    }

    @Override
    protected void setColors(int color) {
        super.setColors(color);
        albumTitle.setTextColor(ToolbarContentTintHelper.toolbarTitleColor(this, color));
    }
}

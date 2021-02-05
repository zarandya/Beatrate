package com.poupa.vinylmusicplayer.ui.activities.tageditor;

import android.content.BroadcastReceiver;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kabouzeid.appthemehelper.util.ToolbarContentTintHelper;

import io.github.zarandya.beatrate.BeatDetectionService;
import io.github.zarandya.beatrate.R;
import com.poupa.vinylmusicplayer.discog.Discography;
import com.poupa.vinylmusicplayer.model.Song;

import org.jaudiotagger.tag.FieldKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import kotlin.Unit;

import static com.poupa.vinylmusicplayer.model.Song.BpmType.DISABLED;
import static com.poupa.vinylmusicplayer.model.Song.BpmType.INVALID;
import static com.poupa.vinylmusicplayer.model.Song.BpmType.MANUAL;
import static io.github.zarandya.beatrate.BeatDetectorKt.MAX_BPM;
import static io.github.zarandya.beatrate.BeatDetectorKt.MIN_BPM;
import static io.github.zarandya.beatrate.tags.TagStringKt.updateTagSignature;

public class SongTagEditorActivity extends AbsTagEditorActivity implements TextWatcher, View.OnClickListener {

    @BindView(R.id.title1)
    EditText songTitle;
    @BindView(R.id.title2)
    EditText albumTitle;
    @BindView(R.id.artist)
    EditText artist;
    @BindView(R.id.genre)
    EditText genre;
    @BindView(R.id.year)
    EditText year;
    @BindView(R.id.track_number)
    EditText trackNumber;
    @BindView(R.id.disc_number)
    EditText discNumber;
    @BindView(R.id.lyrics)
    EditText lyrics;

    @BindView(R.id.beat_enabled_check_box)
    CheckBox beatEnabledCheckBox;
    @BindView(R.id.redetect_beat_button)
    View reDetectBeatButton;
    @BindView(R.id.beat_manual_entry_edit)
    EditText beatManualEntryEdit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        setNoImageMode();
        setUpViews();

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(R.string.action_tag_editor);

        BeatDetectionService.registerSongDetectionFinishedReceiver(this, bpmDetectFinishedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(bpmDetectFinishedReceiver);
    }

    private void setUpViews() {
        fillViewsWithFileTags();
        songTitle.addTextChangedListener(this);
        albumTitle.addTextChangedListener(this);
        artist.addTextChangedListener(this);
        genre.addTextChangedListener(this);
        year.addTextChangedListener(this);
        trackNumber.addTextChangedListener(this);
        discNumber.addTextChangedListener(this);
        lyrics.addTextChangedListener(this);
        reDetectBeatButton.setOnClickListener(this);
        beatManualEntryEdit.addTextChangedListener(this);
    }

    private void fillViewsWithFileTags() {
        songTitle.setText(getSongTitle());
        albumTitle.setText(getAlbumTitle());
        artist.setText(getArtistName());
        genre.setText(getGenreName());
        year.setText(getSongYear());
        trackNumber.setText(getTrackNumber());
        discNumber.setText(getDiscNumber());
        lyrics.setText(getLyrics());
        
        Song song = getSongObject();
        fillBpmViewsWithTags(song.bpm, song.bpmType);
    }

    private void fillBpmViewsWithTags(double bpm, int bpmType) {
        if (bpmType == INVALID) {
            beatEnabledCheckBox.setChecked(false);
            beatManualEntryEdit.setHint(getResources().getStringArray(R.array.bpm_types)[INVALID]);
        }
        else {
            beatEnabledCheckBox.setChecked(true);
            beatManualEntryEdit.setHint(bpm + " (" + getResources().getStringArray(R.array.bpm_types)[bpmType] + ")");
        }
    }

    @Override
    protected void loadCurrentImage() {

    }

    /*
    @Override
    protected void getImageFromLastFM() {

    }
     */

    @Override
    protected void searchImageOnWeb() {

    }

    @Override
    protected void deleteImage() {

    }
    
    private void setBeatTags(Map<FieldKey, String> fieldKeyValueMap) {
        int bpmType = DISABLED;
        String newBpmString = "";
        if (beatEnabledCheckBox.isChecked()) {
            Song song = getSongObject();
            newBpmString = beatManualEntryEdit.getText().toString();
            if (newBpmString.equals("")) {
                newBpmString = String.valueOf(song.bpm);
            }
            double newBpm = Double.parseDouble(newBpmString);
            if (newBpm < MIN_BPM || newBpm > MAX_BPM)
                return;
            fieldKeyValueMap.put(FieldKey.BPM, newBpmString);
            bpmType = (newBpm == song.bpm ? song.bpmType : MANUAL);
        }

        final String custom1 = getAudioFile(getSongPaths().get(0)).getTagOrCreateAndSetDefault().getFirst(FieldKey.CUSTOM1);
        fieldKeyValueMap.put(FieldKey.CUSTOM1, updateTagSignature(custom1, newBpmString, bpmType));
    }

    @NotNull
    private Song getSongObject() {
        return Discography.getInstance().getSong(getId());
    }

    @Override
    protected void save() {
        Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
        fieldKeyValueMap.put(FieldKey.TITLE, songTitle.getText().toString());
        fieldKeyValueMap.put(FieldKey.ALBUM, albumTitle.getText().toString());
        fieldKeyValueMap.put(FieldKey.ARTIST, artist.getText().toString());
        fieldKeyValueMap.put(FieldKey.GENRE, genre.getText().toString());
        fieldKeyValueMap.put(FieldKey.YEAR, year.getText().toString());
        fieldKeyValueMap.put(FieldKey.TRACK, trackNumber.getText().toString());
        fieldKeyValueMap.put(FieldKey.DISC_NO, discNumber.getText().toString());
        fieldKeyValueMap.put(FieldKey.LYRICS, lyrics.getText().toString());
        setBeatTags(fieldKeyValueMap);
        writeValuesToFiles(fieldKeyValueMap, null);
    }

    @Override
    protected int getContentViewLayout() {
        return R.layout.activity_song_tag_editor;
    }

    @NonNull
    @Override
    protected List<String> getSongPaths() {
        ArrayList<String> paths = new ArrayList<>(1);
        paths.add(getSongObject().data);
        return paths;
    }

    @Override
    protected void loadImageFromFile(Uri imageFilePath) {

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
        int toolbarTitleColor = ToolbarContentTintHelper.toolbarTitleColor(this, color);
        songTitle.setTextColor(toolbarTitleColor);
        albumTitle.setTextColor(toolbarTitleColor);
    }

    @Override
    public void onClick(View v) {
        BeatDetectionService.startActionAddSong(this, getSongObject(), BeatDetectionService.PLAY_NOW); // TODO this needs its own priority
    }

    private final BroadcastReceiver bpmDetectFinishedReceiver =
            BeatDetectionService.getNewOnSongDetectionFinishedReceiver((song, success) -> {
                if (song.id == getId()) {
                    if (success) {
                        fillBpmViewsWithTags(song.bpm, song.bpmType);
                    }
                    else {
                        Toast.makeText(this, R.string.toast_failed_to_detect_beat, Toast.LENGTH_SHORT).show();
                    }
                }
                return Unit.INSTANCE; // Arrgh... Kotlin lambdas require returning kotlin.Unit when called from Java
            });
}

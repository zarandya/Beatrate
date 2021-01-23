package com.poupa.vinylmusicplayer.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import io.github.zarandya.beatrate.R;
import com.poupa.vinylmusicplayer.glide.GlideApp;
import com.poupa.vinylmusicplayer.glide.VinylColoredTarget;
import com.poupa.vinylmusicplayer.glide.VinylGlideExtension;
import com.poupa.vinylmusicplayer.misc.CustomFragmentStatePagerAdapter;
import com.poupa.vinylmusicplayer.model.Song;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.poupa.vinylmusicplayer.util.ViewUtil.VINYL_ALBUM_ART_SCALE_TYPE;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AlbumCoverPagerAdapter extends CustomFragmentStatePagerAdapter {

    private ArrayList<Song> dataSet;

    private AlbumCoverFragment.ColorReceiver currentColorReceiver;
    private int currentColorReceiverPosition = -1;

    public AlbumCoverPagerAdapter(FragmentManager fm, ArrayList<Song> dataSet) {
        super(fm);
        this.dataSet = dataSet;
    }

    @Override
    public Fragment getItem(final int position) {
        return AlbumCoverFragment.newInstance(dataSet.get(position));
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    @NonNull
    public Object instantiateItem(ViewGroup container, int position) {
        Object o = super.instantiateItem(container, position);
        if (currentColorReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentColorReceiver, currentColorReceiverPosition);
        }
        return o;
    }

    /**
     * Only the latest passed {@link AlbumCoverFragment.ColorReceiver} is guaranteed to receive a response
     */
    public void receiveColor(AlbumCoverFragment.ColorReceiver colorReceiver, int position) {
        AlbumCoverFragment fragment = (AlbumCoverFragment) getFragment(position);
        if (fragment != null) {
            currentColorReceiver = null;
            currentColorReceiverPosition = -1;
            fragment.receiveColor(colorReceiver, position);
        } else {
            currentColorReceiver = colorReceiver;
            currentColorReceiverPosition = position;
        }
    }

    public static class AlbumCoverFragment extends Fragment {
        private static final String SONG_ARG = "song";

        private Unbinder unbinder;

        @BindView(R.id.player_image)
        ImageView albumCover;

        private boolean isColorReady;
        private int color;
        private Song song;
        private ColorReceiver colorReceiver;
        private int request;

        public static AlbumCoverFragment newInstance(final Song song) {
            AlbumCoverFragment frag = new AlbumCoverFragment();
            final Bundle args = new Bundle();
            args.putParcelable(SONG_ARG, song);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            song = getArguments().getParcelable(SONG_ARG);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_album_cover, container, false);
            unbinder = ButterKnife.bind(this, view);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            albumCover.setScaleType(VINYL_ALBUM_ART_SCALE_TYPE);
            loadAlbumCover();
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            unbinder.unbind();
            colorReceiver = null;
        }

        private void loadAlbumCover() {
            GlideApp.with(this)
                    .asBitmapPalette()
                    .load(VinylGlideExtension.getSongModel(song))
                    .transition(VinylGlideExtension.getDefaultTransition())
                    .songOptions(song)
                    .into(new VinylColoredTarget(albumCover) {
                        @Override
                        public void onColorReady(int color) {
                            setColor(color);
                        }
                    });
        }

        private void setColor(int color) {
            this.color = color;
            isColorReady = true;
            if (colorReceiver != null) {
                colorReceiver.onColorReady(color, request);
                colorReceiver = null;
            }
        }

        public void receiveColor(ColorReceiver colorReceiver, int request) {
            if (isColorReady) {
                colorReceiver.onColorReady(color, request);
            } else {
                this.colorReceiver = colorReceiver;
                this.request = request;
            }
        }

        public interface ColorReceiver {
            void onColorReady(int color, int request);
        }
    }
}


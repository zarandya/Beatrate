package com.poupa.vinylmusicplayer.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import io.github.zarandya.beatrate.R;
import com.poupa.vinylmusicplayer.adapter.PlaylistAdapter;
import com.poupa.vinylmusicplayer.interfaces.LoaderIds;
import com.poupa.vinylmusicplayer.loader.PlaylistLoader;
import com.poupa.vinylmusicplayer.misc.WrappedAsyncTaskLoader;
import com.poupa.vinylmusicplayer.model.Playlist;
import com.poupa.vinylmusicplayer.model.smartplaylist.HistoryPlaylist;
import com.poupa.vinylmusicplayer.model.smartplaylist.LastAddedPlaylist;
import com.poupa.vinylmusicplayer.model.smartplaylist.MyTopTracksPlaylist;
import com.poupa.vinylmusicplayer.model.smartplaylist.NotRecentlyPlayedPlaylist;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class PlaylistsFragment extends AbsLibraryPagerRecyclerViewFragment<PlaylistAdapter, LinearLayoutManager> implements LoaderManager.LoaderCallbacks<ArrayList<Playlist>> {

    private static final int LOADER_ID = LoaderIds.PLAYLISTS_FRAGMENT;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @NonNull
    @Override
    protected PlaylistAdapter createAdapter() {
        ArrayList<Playlist> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new PlaylistAdapter(getLibraryFragment().getMainActivity(), dataSet, R.layout.item_list_single_row, getLibraryFragment());
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_playlists;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<ArrayList<Playlist>> onCreateLoader(int id, Bundle args) {
        return new AsyncPlaylistLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Playlist>> loader, ArrayList<Playlist> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Playlist>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncPlaylistLoader extends WrappedAsyncTaskLoader<ArrayList<Playlist>> {
        public AsyncPlaylistLoader(Context context) {
            super(context);
        }

        private static ArrayList<Playlist> getAllPlaylists(Context context) {
            ArrayList<Playlist> playlists = new ArrayList<>();

            playlists.add(new LastAddedPlaylist(context));
            playlists.add(new HistoryPlaylist(context));
            playlists.add(new NotRecentlyPlayedPlaylist(context));
            playlists.add(new MyTopTracksPlaylist(context));

            playlists.addAll(PlaylistLoader.getAllPlaylists(context));

            return playlists;
        }

        @Override
        public ArrayList<Playlist> loadInBackground() {
            return getAllPlaylists(getContext());
        }
    }
}

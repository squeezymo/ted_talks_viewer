package com.squeezymo.tedviewer.ui.fragments;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;

import com.squeezymo.tedviewer.R;

import java.net.URI;

public class VideoPlaybackFragment extends Fragment {
    private static final String LOG_TAG = VideoPlaybackFragment.class.getCanonicalName();

    private VideoView mVideoView;
    private int mPositionRetained;
    private boolean mPausedRetained;
    private Uri mUriRetained;

    public static VideoPlaybackFragment instantiate(Bundle args) {
        VideoPlaybackFragment fragment = new VideoPlaybackFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static VideoPlaybackFragment instantiate() {
        return VideoPlaybackFragment.instantiate(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        mVideoView = (VideoView) view.findViewById(R.id.video);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPositionRetained = mVideoView.getCurrentPosition();
        mPausedRetained = !mVideoView.isPlaying();
    }

    public void playVideo(URI uri) {
        MediaController mc = new MediaController(getActivity());
        mc.setAnchorView(mVideoView);
        mc.setMediaPlayer(mVideoView);

        Uri videoURI = Uri.parse(uri.toString());

        mVideoView.setMediaController(mc);
        mVideoView.setVideoURI(videoURI);

        if (videoURI.equals(mUriRetained)) {
            mVideoView.seekTo(mPositionRetained);
            if (!mPausedRetained) mVideoView.start();
        }
        else {
            mVideoView.start();
        }

        mUriRetained = videoURI;
    }
}

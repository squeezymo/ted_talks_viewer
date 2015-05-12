package com.squeezymo.tedviewer.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import com.squeezymo.tedviewer.R;
import com.squeezymo.tedviewer.ui.fragments.FeedFragment;
import com.squeezymo.tedviewer.ui.fragments.VideoPlaybackFragment;

import java.net.URI;

public class VideoPlaybackActivity extends Activity {
    private static final String LOG_TAG = VideoPlaybackActivity.class.getCanonicalName();
    private static final String TAG_VIDEO_FRAGMENT = VideoPlaybackActivity.class.getSimpleName() + "." + VideoPlaybackFragment.class.getSimpleName();
    private static final String TAG_FEED_FRAGMENT = VideoPlaybackActivity.class.getSimpleName() + "." + FeedFragment.class.getSimpleName();

    public static final String EXTRA_URI = VideoPlaybackActivity.class.getCanonicalName() + ".extra.URI";
    public static final String EXTRA_TITLE = VideoPlaybackActivity.class.getCanonicalName() + ".extra.TITLE";
    public static final String EXTRA_DESCRIPTION = VideoPlaybackActivity.class.getCanonicalName() + ".extra.DESCRIPTION";

    private FragmentManager mFragmentManager;
    private VideoPlaybackFragment mVideoFragment;
    private FeedFragment mFeedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mFragmentManager = getFragmentManager();

        mVideoFragment = (VideoPlaybackFragment) mFragmentManager.findFragmentByTag(TAG_VIDEO_FRAGMENT);

        if (mVideoFragment == null) {
            mVideoFragment = VideoPlaybackFragment.instantiate();
            mFragmentManager.beginTransaction().add(R.id.video_container, mVideoFragment, TAG_VIDEO_FRAGMENT).commit();
        }

        if (findViewById(R.id.feed_container) != null) {
            mFeedFragment = (FeedFragment) mFragmentManager.findFragmentByTag(TAG_FEED_FRAGMENT);

            if (mFeedFragment == null) {
                mFeedFragment = FeedFragment.instantiate();
                mFragmentManager.beginTransaction().add(R.id.feed_container, mFeedFragment, TAG_FEED_FRAGMENT).commit();
            }

            TextView titleView = (TextView) findViewById(R.id.title);
            titleView.setText(getIntent().getStringExtra(EXTRA_TITLE));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (findViewById(R.id.feed_container) != null) {
            if (mFeedFragment != null) {
                mFragmentManager.beginTransaction().remove(mFeedFragment).commit();
            }
        }

        super.onSaveInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        URI uri = (URI) getIntent().getExtras().get(EXTRA_URI);
        mVideoFragment.playVideo(uri);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFeedFragment != null) {
            mFeedFragment.retainEntries();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) finish();
        return super.onKeyDown(keyCode, event);
    }

}
package com.squeezymo.tedviewer.ui;

import android.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import com.squeezymo.tedviewer.R;
import com.squeezymo.tedviewer.ui.fragments.FeedFragment;

public class MainActivity extends ActionBarActivity {
    private static final String LOG_TAG = MainActivity.class.getCanonicalName();
    private static final String TAG_FEED_FRAGMENT = MainActivity.class.getSimpleName() + "." + FeedFragment.class.getSimpleName();

    private FragmentManager mFragmentManager;
    private FeedFragment mFeedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentManager = getFragmentManager();
        mFeedFragment = (FeedFragment) mFragmentManager.findFragmentByTag(TAG_FEED_FRAGMENT);

        if (mFeedFragment == null) {
            mFeedFragment = FeedFragment.instantiate();
            mFragmentManager.beginTransaction().add(R.id.feed_container, mFeedFragment, TAG_FEED_FRAGMENT).commit();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFeedFragment != null) {
            mFeedFragment.retainEntries();
        }
    }
}

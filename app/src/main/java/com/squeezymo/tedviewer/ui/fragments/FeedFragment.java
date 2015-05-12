package com.squeezymo.tedviewer.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.gc.materialdesign.views.ButtonFloat;
import com.gc.materialdesign.views.ButtonRectangle;
import com.gc.materialdesign.widgets.SnackBar;
import com.squeezymo.tedviewer.R;
import com.squeezymo.tedviewer.rss.TedClient;
import com.squeezymo.tedviewer.ui.adapters.FeedListAdapter;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import java.util.List;

public class FeedFragment extends Fragment {
    private static final String LOG_TAG = FeedFragment.class.getCanonicalName();
    private static final int SNACKBAR_DURATION = 4000;

    private static List<SyndEntry> sEntriesRetained;
    private RecyclerView mRecyclerView;
    private LinearLayout mProgressBar;
    private FeedListAdapter mViewAdapter;
    private Button mBtnRetry;
    private StickyRecyclerHeadersDecoration mHeadersDecor;
    private Handler mHandler;

    public static FeedFragment instantiate(Bundle args) {
        FeedFragment fragment = new FeedFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static FeedFragment instantiate() {
        return FeedFragment.instantiate(null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);

        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list_view);
        mRecyclerView.setHasFixedSize(true);

        mProgressBar = (LinearLayout) view.findViewById(R.id.progressbar);

        mBtnRetry = (Button) view.findViewById(R.id.btn_retry);

        mBtnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnRetry.setVisibility(View.GONE);
                setLoadingDataIndicator(true);
                TedClient.requestAsyncUpdate(mHandler, getActivity());
            }
        });

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case TedClient.MSG_FEED_RETRIEVED:
                        if (inputMessage.obj == null || !(inputMessage.obj instanceof SyndFeed)) {
                            return;
                        }

                        final SyndFeed feed = (SyndFeed) inputMessage.obj;
                        mViewAdapter.setEntries(feed.getEntries());
                        setLoadingDataIndicator(false);

                        break;
                    case TedClient.MSG_NO_CONNECTION:
                        setLoadingDataIndicator(false);

                        SnackBar snackBar = new SnackBar(
                                getActivity(),
                                getActivity().getResources().getString(R.string.result_no_connection),
                                null, null
                        );
                        snackBar.setDismissTimer(SNACKBAR_DURATION);
                        snackBar.show();

                        mBtnRetry.setVisibility(View.VISIBLE);

                        break;
                    case TedClient.MSG_ERR:
                        setLoadingDataIndicator(false);

                        snackBar = new SnackBar(
                                getActivity(),
                                getActivity().getResources().getString(R.string.result_error),
                                null, null
                        );
                        snackBar.setDismissTimer(SNACKBAR_DURATION);
                        snackBar.show();

                        mBtnRetry.setVisibility(View.VISIBLE);

                        break;
                }
            }
        };

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(llm);

        mViewAdapter = new FeedListAdapter(getActivity());
        mViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mHeadersDecor.invalidateHeaders();
            }
        });

        mHeadersDecor = new StickyRecyclerHeadersDecoration(mViewAdapter);

        mRecyclerView.setAdapter(mViewAdapter);
        mRecyclerView.addItemDecoration(mHeadersDecor);

        if (sEntriesRetained == null) {
            setLoadingDataIndicator(true);
            TedClient.requestAsyncUpdate(mHandler, getActivity());
        }
        else {
            mViewAdapter.setEntries(sEntriesRetained);
            setLoadingDataIndicator(false);
        }

        sEntriesRetained = null;
    }

    private void setLoadingDataIndicator(boolean loading) {
        mRecyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        mProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        retainEntries();
    }

    public void retainEntries() {
        sEntriesRetained = mViewAdapter.getEntries();
    }
}

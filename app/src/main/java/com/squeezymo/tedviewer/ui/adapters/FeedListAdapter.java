package com.squeezymo.tedviewer.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.RecyclerView;

import android.text.Html;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.squeezymo.tedviewer.R;
import com.squeezymo.tedviewer.rss.TedClient;
import com.squeezymo.tedviewer.ui.VideoPlaybackActivity;
import com.sun.syndication.feed.synd.SyndEntry;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FeedListAdapter
        extends RecyclerView.Adapter<FeedListAdapter.EntryViewHolder>
        implements StickyRecyclerHeadersAdapter<FeedListAdapter.HeaderViewHolder> {

    private static final String LOG_TAG = FeedListAdapter.class.getCanonicalName();

    private List<SyndEntry> mEntries;
    private final Context mContext;
    private final Map<String, Integer> mPositionByUri;
    private final Handler mHandler;

    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView date;

        public HeaderViewHolder(View v) {
            super(v);
            date = (TextView) v.findViewById(R.id.date);
        }

        private void bindItem(long year) {
            date.setText(Long.toString(year));
        }
    }

    public class EntryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private static final String DATE_PATTERN_UI = "d MMMM";

        private SyndEntry entry;
        private TextView pubDateView;
        private ImageView thumbnailView;
        private TextView titleView;
        private ExpandableTextView descriptionView;
        private TextView durationView;

        public EntryViewHolder(View v) {
            super(v);

            pubDateView = (TextView) v.findViewById(R.id.pubDate);
            thumbnailView = (ImageView) v.findViewById(R.id.thumbnail);
            titleView = (TextView) v.findViewById(R.id.title);
            descriptionView = (ExpandableTextView) v.findViewById(R.id.description);
            durationView = (TextView) v.findViewById(R.id.duration);

            v.setOnClickListener(this);
        }

        private void bindItem(SyndEntry entry) {
            this.entry = entry;

            Bitmap thumbnail = TedClient.ThumbnailManager.requestThumbnail(entry, mHandler, mContext);
            thumbnailView.setImageDrawable(new BitmapDrawable(mContext.getResources(), thumbnail));

            DateFormat format = new SimpleDateFormat(DATE_PATTERN_UI);
            pubDateView.setText(format.format(entry.getPublishedDate()));

            titleView.setText(entry.getTitle());
            descriptionView.setText(Html.fromHtml(entry.getDescription().getValue()));

            durationView.setText(TedClient.VideoManager.getDurationString(entry));
        }

        @Override
        public void onClick(View v) {
            final List<TedClient.VideoManager.Video> videos = TedClient.VideoManager.getVideos(entry);
            final String[] options = new String[videos.size()];

            for (int i = 0; i < options.length; i++) {
                TedClient.VideoManager.Video video = videos.get(i);
                options[i] = mContext.getResources().getString(
                        R.string.dialog_quality_option,
                        video.getBitrate(), Formatter.formatShortFileSize(mContext, video.getFileSize())
                );
            }

            new AlertDialog.Builder(mContext)
                    .setTitle(mContext.getResources().getString(R.string.dialog_quality))
                    .setItems(
                            options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    TedClient.VideoManager.Video video = videos.get(i);
                                    URI uri = video.getURI();

                                    if (uri != null) {
                                        Intent intent = new Intent(mContext, VideoPlaybackActivity.class);
                                        intent.putExtra(VideoPlaybackActivity.EXTRA_URI, video.getURI());
                                        intent.putExtra(VideoPlaybackActivity.EXTRA_TITLE, entry.getTitle());
                                        intent.putExtra(VideoPlaybackActivity.EXTRA_DESCRIPTION, entry.getDescription().getValue());

                                        mContext.startActivity(intent);
                                    }
                                    else {
                                        Log.e(LOG_TAG, "Could not fetch URI");
                                    }
                                }
                            }
                    ).create().show();
        }
    }

    public FeedListAdapter(Context context) {
        mContext = context;
        mPositionByUri = new HashMap<>();
        mEntries = new LinkedList<>();

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                switch (inputMessage.what) {
                    case TedClient.MSG_IMG_UPDATED:
                        notifyEntryChanged((SyndEntry) inputMessage.obj);
                        break;
                }
            }
        };
    }

    public void setEntries(List<SyndEntry> entries) {
        if (entries == null) {
            mEntries.clear();
        }
        else {
            mEntries = entries;
            Collections.sort(mEntries, new Comparator<SyndEntry>() {
                @Override
                public int compare(SyndEntry entry1, SyndEntry entry2) {
                    return entry2.getPublishedDate().compareTo(entry1.getPublishedDate());
                }
            });
        }

        mPositionByUri.clear();
        notifyDataSetChanged();
    }

    public List<SyndEntry> getEntries() {
        return mEntries;
    }

    @Override
    public long getHeaderId(int position) {
        SyndEntry entry = mEntries.get(position);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(entry.getPublishedDate());

        return calendar.get(Calendar.YEAR);
    }

    @Override
    public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.header_viewholder, viewGroup, false);
        return new HeaderViewHolder(itemView);
    }

    @Override
    public EntryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.entry_viewholder, viewGroup, false);
        return new EntryViewHolder(itemView);
    }

    @Override
    public void onBindHeaderViewHolder(HeaderViewHolder holder, int position) {
        holder.bindItem(getHeaderId(position));
    }

    @Override
    public void onBindViewHolder(EntryViewHolder holder, int position) {
        SyndEntry entry = mEntries.get(position);
        mPositionByUri.put(entry.getUri(), position);
        holder.bindItem(entry);
    }

    @Override
    public int getItemCount() {
        return mEntries.size();
    }

    private void notifyEntryChanged(SyndEntry entry) {
        if (entry == null)
            return;

        if (mPositionByUri.containsKey(entry.getUri())) {
            notifyItemChanged(mPositionByUri.get(entry.getUri()));
        }
    }
}

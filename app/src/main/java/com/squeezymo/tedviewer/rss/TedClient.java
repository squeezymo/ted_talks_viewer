package com.squeezymo.tedviewer.rss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.squeezymo.tedviewer.R;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.MediaGroup;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TedClient {
    private static final String LOG_TAG = TedClient.class.getCanonicalName();

    public static final int MSG_FEED_RETRIEVED = 0x10;
    public static final int MSG_NO_CONNECTION = 0x11;
    public static final int MSG_ERR = 0x12;
    public static final int MSG_IMG_UPDATED = 0x13;

    private static final ExecutorService cThreadPool = Executors.newCachedThreadPool();

    public static void requestAsyncUpdate(final Handler callback, final Context context) {
        Runnable downloader = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    if (!hasConnectivity(callback, context)) {
                        return;
                    }

                    try {
                        SyndFeedInput input = new SyndFeedInput();
                        SyndFeed feed = input.build(new XmlReader(new URL("http://www.ted.com/themes/rss/id/6")));

                        if (callback != null) {
                            Message.obtain(callback, MSG_FEED_RETRIEVED, feed).sendToTarget();
                        }
                    }
                    catch (IOException|FeedException e) {
                        // TODO handle e

                        if (callback != null) {
                            Message.obtain(callback, MSG_ERR, e).sendToTarget();
                        }
                    }
                }
                finally {
                    Looper.loop();
                }
            }
        };

        cThreadPool.execute(downloader);
    }

    public static class ThumbnailManager {
        private final static Map<String, Bitmap> sThumbnailByUri = Collections.synchronizedMap(new WeakHashMap<String, Bitmap>());

        private static Thumbnail getThumbnail(final SyndEntry entry) {
            MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
            if (media.getMediaGroups() == null || media.getMediaGroups().length < 1)
                return null;

            MediaGroup group = media.getMediaGroups()[0];

            Metadata metadata = group.getMetadata();
            if (metadata.getThumbnail() == null || metadata.getThumbnail().length < 1)
                return null;

            return metadata.getThumbnail()[0];
        }

        public static Bitmap requestThumbnail(final SyndEntry entry, final Handler callback, final Context context) {
            if (sThumbnailByUri.containsKey(entry.getUri())) {
                return sThumbnailByUri.get(entry.getUri());
            }

            Runnable downloader = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();

                    try {
                        Thumbnail thumbnail = getThumbnail(entry);
                        if (thumbnail == null)
                            return;

                        if (!hasConnectivity(callback, context))
                            return;

                        try (InputStream stream = thumbnail.getUrl().toURL().openConnection().getInputStream()) {
                            Bitmap bitmap = BitmapFactory.decodeStream(stream);
                            bitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, false);
                            sThumbnailByUri.put(entry.getUri(), bitmap);

                            if (callback != null) {
                                Message.obtain(callback, MSG_IMG_UPDATED, entry).sendToTarget();
                            }
                        } catch (IOException e) {
                            // TODO handle e
                            e.printStackTrace();
                        }
                    }
                    finally {
                        Looper.loop();
                    }
                }
            };

            cThreadPool.execute(downloader);

            return Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.ted_logo), 150, 150, false);
        }
    }

    public static class VideoManager {
        public static class Video {
            private URI uri;
            private long duration;
            private long fileSize;
            private int bitrate;

            private Video(URI uri, long duration, long fileSize, int bitrate) {
                this.uri = uri;
                this.duration = duration;
                this.fileSize = fileSize;
                this.bitrate = bitrate;
            }

            public int getBitrate() {
                return bitrate;
            }

            public URI getURI() {
                return uri;
            }

            public long getFileSize() {
                return fileSize;
            }

            public long getDuration() {
                return duration;
            }
        }

        public static List<Video> getVideos(SyndEntry entry) {
            List<Video> videos = new ArrayList<>();

            MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
            if (media.getMediaGroups() == null || media.getMediaGroups().length < 1)
                return videos;

            for (MediaContent content : media.getMediaGroups()[0].getContents()) {
                UrlReference ref = (UrlReference) content.getReference();
                videos.add(new Video(ref.getUrl(), content.getDuration(), content.getFileSize(), content.getBitrate().intValue()));
            }

            return videos;
        }

        public static String getDurationString(SyndEntry entry) {
            MediaEntryModule media = (MediaEntryModule) entry.getModule(MediaEntryModule.URI);
            if (media.getMediaGroups() == null || media.getMediaGroups().length < 1)
                return "";

            MediaContent content = media.getMediaGroups()[0].getContents()[0];
            long seconds = content.getDuration();
            long hours = seconds/3600;
            if (hours > 0) {
                seconds %= 3600;
                long minutes = seconds/60;
                seconds = seconds%60;
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            }
            else {
                long minutes = seconds/60;
                seconds = seconds%60;
                return String.format("%02d:%02d", minutes, seconds);
            }
        }
    }

    public static boolean hasConnectivity(final Handler callback, final Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if ( connectivityManager.getActiveNetworkInfo() == null ||
                !connectivityManager.getActiveNetworkInfo().isAvailable() ||
                !connectivityManager.getActiveNetworkInfo().isConnected() ) {

            Message.obtain(callback, MSG_NO_CONNECTION, null).sendToTarget();
            return false;

        }

        return true;
    }
}

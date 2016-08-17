package ru.yaklimenko.webimagesgrabber;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Антон on 16.08.2016.
 * connection and page downloading manager
 */
public class PageLoadManager {

    private static final String TAG = PageLoadManager.class.getSimpleName();
    private static final int TIMEOUT_SECONDS = 15;

    private static PageLoadManager instance;

    PageResult pageResult;

    PageLoadTask pageLoadTask;

    private OnPageLoadedListener onPageLoadedListener;

    private PageLoadManager(){/*empty*/}

    public static PageLoadManager getInstance() {
        if (instance == null) {
            instance = new PageLoadManager();
        }

        return instance;
    }

    public void requestHtmlPage(String url, OnPageLoadedListener listener) {
        onPageLoadedListener = listener;

        if (url == null || url.isEmpty()) {
            listener.onPageLoadingError(new IllegalArgumentException("empty url"));
            return;
        }

        if (pageResult != null && url.equals(pageResult.url)) {
            if (pageResult.isReady()) {
                pageResult.notifyListener();
            }
        } else {
            pageResult = new PageResult(url);
            if (pageLoadTask != null && pageLoadTask.getStatus() != AsyncTask.Status.FINISHED) {
                pageLoadTask.cancel(true);
                Log.w(TAG, "requestHtmlPage: canceled");
            }
            new PageLoadTask().execute(url);
        }

    }

    private class PageLoadTask extends AsyncTask<String, Void, String> {

        Throwable throwable;
        String processingUrl;

        @Override
        protected String doInBackground(String... strings) {
            String pageUrl = strings[0];
            processingUrl = pageUrl;
            if (!pageUrl.contains(Constants.SCHEMA_SEPARATOR)) {
                String pageContent = null;
                for (String protocolSchema : Constants.SUPPORTED_PROTOCOLS) {
                    String tmpUrl = protocolSchema + Constants.SCHEMA_SEPARATOR + pageUrl;
                    try {
                        pageContent = downloadPage(tmpUrl);
                    } catch (UnknownHostException e) {
                        /*ignore*/
                    } catch (Exception e) {
                        throwable = e;
                        return null;
                    }

                    if (pageContent != null) {
                        parseBaseUrlForPageResult(tmpUrl);
                        return pageContent;
                    }
                }
                throwable = new IllegalStateException("cannot resolve remote host");
                return null;
            }

            try {
                return downloadPage(pageUrl);
            } catch (IOException e) {
                throwable = e;
                return null;
            }
        }

        private void parseBaseUrlForPageResult(String tmpUrl) {
            URL url = null;
            try {
                 url = new URL(tmpUrl);
            } catch (MalformedURLException e) {
                /* can't be */
            }
            if (url != null) {
                String baseUrl =  url.getProtocol() + "://" + url.getHost();
                pageResult.baseUrl = baseUrl;
            }
        }

        private String downloadPage(String url) throws IOException {
            Request request = new Request.Builder().url(url).build();
            ResponseBody body = null;
            try {
                Response response = new OkHttpClient().newBuilder()
                        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .build()
                        .newCall(request).execute();
                body = response.body();
                return body.string();
            } finally {
                if (body != null) {
                    body.close();
                }
            }
        }

        @Override
        protected void onPostExecute(String pageContent) {
            if (isCancelled()) {
                return;
            }
            pageResult.throwable = throwable;
            pageResult.pageContent = pageContent;
            pageResult.notifyListener();

        }
    }

    public static void getPreparedImagesUrlsFromPage (
            String pageContent, String baseUrl, OnImagesUrlsPreparedListener listener
    ) {
        ImageExtractionTask imageExtractionTask = new ImageExtractionTask();
        imageExtractionTask.execute(pageContent, baseUrl);
    }

    private static class ImageExtractionTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... params) {
            String content = params[0];
            String baseUrl = params[1];

            List<String> unpreparedUrls = getImagesUrlsFromPage(content, baseUrl);


            return null;
        }



        @Override
        protected void onPostExecute(List<String> urls) {
            super.onPostExecute(urls);
        }
    }



    private static List<String> getImagesUrlsFromPage(String pageContent, String baseUrl) {

        Document doc = Jsoup.parse(pageContent);
        doc.setBaseUri(baseUrl);
        Elements images = doc.select("img[src]");
        List<String> urls = new ArrayList<>(images.size());
        for (Element imageElement : images) {
            String src =  imageElement.attr("abs:src");
            if (!src.isEmpty()) {
                urls.add(src);
            }
        }
        Log.d(TAG, "parsed " + urls.size() + " images");
        return urls;
    }

    public interface OnImagesUrlsPreparedListener {
        void onImagesUrlsPrepared (List<String> imagesUrls);
    }

    private class PageResult {
        String url;
        String baseUrl;
        String pageContent;
        Throwable throwable;

        private PageResult(String url) {
            this.url = url;
        }

        private boolean isReady() {
            return pageContent != null || throwable != null;
        }

        private void notifyListener () {
            if (!isReady()) {
                Log.e(TAG, "notifyListener: no result yet");
            }
            if (pageContent != null) {
                onPageLoadedListener.onPageLoaded(pageContent, baseUrl);
            } else {
                onPageLoadedListener.onPageLoadingError(throwable);
            }
        }
    }

    public interface OnPageLoadedListener {
        void onPageLoaded(String pageContent, String baseUrl);
        void onPageLoadingError(Throwable throwable);
    }

}

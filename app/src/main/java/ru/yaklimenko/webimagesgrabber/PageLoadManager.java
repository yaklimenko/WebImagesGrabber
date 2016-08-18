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

    private class PageLoadTask extends AsyncTask<String, Void, List<String>> {

        Throwable throwable;
        String processingUrl;

        @Override
        protected List<String> doInBackground(String... strings) {
            String pageUrl = strings[0];
            processingUrl = pageUrl;
            if (!pageUrl.contains(Constants.SCHEMA_SEPARATOR)) {

                for (String protocolSchema : Constants.SUPPORTED_PROTOCOLS) {
                    String tmpUrl = protocolSchema + Constants.SCHEMA_SEPARATOR + pageUrl;
                    List<String> urls;
                    try {
                        urls = downloadPage(tmpUrl);
                        return urls;
                    } catch (UnknownHostException e) {
                        //ignore
                    } catch (Exception e) {
                        throwable = e;
                        return null;
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

        private String getBaseUrl(String tmpUrl) {
            URL url = null;
            try {
                 url = new URL(tmpUrl);
            } catch (MalformedURLException e) {
                /* can't be */
            }
            if (url != null) {
                return url.getProtocol() + "://" + url.getHost();
            }
            return  null;
        }

        private List<String> downloadPage(String url) throws IOException {
            Document doc = Jsoup.connect(url).get();
            doc.setBaseUri(getBaseUrl(url));
            Elements images = doc.select("img[src]");
            List<String> urls = new ArrayList<>(images.size());
            for (Element imageElement : images) {
                String src =  imageElement.attr("abs:src");
                if (!src.isEmpty()) {
                    urls.add(src);
                }
            }
            return urls;
        }

        @Override
        protected void onPostExecute(List<String> imgUrls) {
            if (isCancelled()) {
                return;
            }
            pageResult.throwable = throwable;
            pageResult.imagesUrls = imgUrls;
            pageResult.notifyListener();

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



    private class PageResult {
        String url;


        List<String> imagesUrls;
        Throwable throwable;

        private PageResult(String url) {
            this.url = url;
        }

        private boolean isReady() {
            return imagesUrls != null || throwable != null;
        }

        private void notifyListener () {
            if (!isReady()) {
                Log.e(TAG, "notifyListener: no result yet");
            }
            if (imagesUrls != null) {
                onPageLoadedListener.onImagesUrlsPrepared(imagesUrls);
            } else {
                onPageLoadedListener.onPageLoadingError(throwable);
            }
        }
    }

    public interface OnPageLoadedListener {
        void onImagesUrlsPrepared (List<String> imagesUrls);
        void onPageLoadingError(Throwable throwable);
    }

}

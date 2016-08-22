package ru.yaklimenko.webimagesgrabber;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_LOADED_URLS = "key_loaded_urls";

    Button submitUrlButton;
    EditText editUrl;
    LinearLayout progressBar;
    ListView imagesListView;

    String[] loadedUrls;

    PageLoadManager.OnPageLoadedListener onPageLoadedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        initListeners();
        readSavedState(savedInstanceState);
    }

    private void readSavedState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_LOADED_URLS)) {
            loadedUrls = savedInstanceState.getStringArray(KEY_LOADED_URLS);
            drawImages(loadedUrls);
        }
    }

    private void initListeners() {
        submitUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmitButtonClicked();
            }
        });

        onPageLoadedListener = new PageLoadManager.OnPageLoadedListener() {
                    @Override
                    public void onImagesUrlsPrepared(String[] imagesUrls) {
                        loadedUrls = imagesUrls;
                        drawImages(loadedUrls);
                    }

                    @Override
                    public void onPageLoadingError(Throwable throwable) {
                        toggleViews(false, false);
                        showErrorDialog(throwable.getMessage());
                    }
                };
    }

    private void onSubmitButtonClicked() {
        toggleViews(false, true);
        PageLoadManager pageLoadManager = PageLoadManager.getInstance();
        pageLoadManager.requestHtmlPage(editUrl.getText().toString().trim(), onPageLoadedListener);
    }



    private void drawImages(final String[] imagesUrls) {

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return imagesUrls.length;
            }

            @Override
            public Object getItem(int i) {
                return imagesUrls[i];
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                View v = view == null ?
                        LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item, viewGroup, false) :
                        view;
                TextView tv = (TextView)v.findViewById(R.id.imgUrl);
                tv.setText(imagesUrls[i]);
                ImageView iv = (ImageView)v.findViewById(R.id.img);

                Glide.with(MainActivity.this)
                        .load(imagesUrls[i])
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(iv);

                return v;
            }
        };
        imagesListView.setAdapter(adapter);
        toggleViews(true, false);
    }

    private void toggleViews(boolean showList, boolean showProgressBar) {
        progressBar.setVisibility(showProgressBar ? View.VISIBLE : View.GONE);
        imagesListView.setVisibility(showList ? View.VISIBLE : View.GONE);
    }



    private void showErrorDialog(String message) {
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ErrorDialogFragment.ARG_ERROR_MESSAGE, message);
        errorDialogFragment.setArguments(args);
        errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    private void bindViews() {
        submitUrlButton = (Button)findViewById(R.id.submitUrlButton);
        editUrl = (EditText)findViewById(R.id.editUrl);
        imagesListView = (ListView)findViewById(R.id.imagesList);
        progressBar = (LinearLayout)findViewById(R.id.progressBar);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (loadedUrls != null) {
            outState.putStringArray(KEY_LOADED_URLS, loadedUrls);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        PageLoadManager.getInstance().removeOnPageLoadedListener(onPageLoadedListener);
        super.onDestroy();
    }
}

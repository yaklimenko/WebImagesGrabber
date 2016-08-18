package ru.yaklimenko.webimagesgrabber;

import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button submitUrlButton;
    EditText editUrl;
    ProgressBar progressBar;
    ScrollView contentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //todo check internet connection

        bindViews();

        submitUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmitButtonClicked();
            }
        });
    }

    private void onSubmitButtonClicked() {
        PageLoadManager pageLoadManager = PageLoadManager.getInstance();
        PageLoadManager.OnPageLoadedListener listener = new PageLoadManager.OnPageLoadedListener() {
            @Override
            public void onImagesUrlsPrepared(List<String> imagesUrls) {
                drawImages(imagesUrls);
            }

            @Override
            public void onPageLoadingError(Throwable throwable) {
                showErrorDialog(throwable.getMessage());
            }
        };
        pageLoadManager.requestHtmlPage(editUrl.getText().toString().trim(), listener);
    }

    private void drawImages(final List<String> imagesUrls) {
        ListView lv = (ListView)LayoutInflater.from(this)
                .inflate(R.layout.list, contentContainer, false);
        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return imagesUrls.size();
            }

            @Override
            public Object getItem(int i) {
                return imagesUrls.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                View v = view == null ? new ImageView(MainActivity.this) : view;
                ImageView iv = (ImageView)v;
                Glide.with(MainActivity.this)
                        .load(imagesUrls.get(i))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(iv);

                return v;
//                View v = view == null ? new TextView(MainActivity.this) : view;
//                TextView tv = (TextView) v;
//                tv.setText(imagesUrls.get(i));
//
//                return v;
            }
        };
        lv.setAdapter(adapter);
        contentContainer.removeAllViews();
        contentContainer.addView(lv);
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
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        contentContainer = (ScrollView)findViewById(R.id.contentContainer);
    }

    /**
     * temp method
     * todo delete me
     */
    private void placePageContentOnActivity(String content) {
        progressBar.setVisibility(View.GONE);
        TextView tv = new TextView(this);
        contentContainer.removeAllViews();
        contentContainer.addView(tv);
        tv.setText(content);
    }
}

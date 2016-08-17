package ru.yaklimenko.webimagesgrabber;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

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
        pageLoadManager.requestHtmlPage(
                editUrl.getText().toString().trim(),
                new PageLoadManager.OnPageLoadedListener() {
            @Override
            public void onPageLoaded(String pageContent, String baseUrl) {
                PageContentService.getPreparedImagesUrlsFromPage(
                        pageContent,
                        baseUrl,
                        new PageContentService.OnImagesUrlsPreparedListener() {
                            @Override
                            public void onImagesUrlsPrepared(List<String> imagesUrls) {

                            }
                });
            }

            @Override
            public void onPageLoadingError(Throwable throwable) {
                showErrorDialog(throwable.getMessage());
            }
        });
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

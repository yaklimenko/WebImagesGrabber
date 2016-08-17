package ru.yaklimenko.webimagesgrabber;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Антон on 16.08.2016.
 */
public class ErrorDialogFragment extends DialogFragment {
    public static final String TAG = ErrorDialogFragment.class.getSimpleName();
    public static final String ARG_ERROR_MESSAGE = "argKeyErrorMessage";

    private String errorMsg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void readArgs() {
        if (getArguments() == null) {
            Log.e(TAG, "readArgs: no message");
            return;
        }
        errorMsg = getArguments().getString(ARG_ERROR_MESSAGE);
        if (errorMsg == null) {
            Log.e(TAG, "readArgs: no message");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        readArgs();
        if (errorMsg == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.errorDialogTitle)
                .setMessage(errorMsg)
                .show();
    }
}

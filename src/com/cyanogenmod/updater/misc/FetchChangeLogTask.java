/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.cyanogenmod.updater.misc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.cyanogenmod.updater.R;
import com.cyanogenmod.updater.NotifyingWebView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class FetchChangeLogTask extends AsyncTask<String, Void, Void>
        implements DialogInterface.OnDismissListener {
    private static final String TAG = "FetchChangeLogTask";

    private Context mContext;
    private String mFileName;
    private NotifyingWebView mChangeLogView;
    private AlertDialog mAlertDialog;

    public FetchChangeLogTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... fileNames) {
        Resources res = mContext.getResources();
        StringBuilder builder = new StringBuilder();

        for(String fileName : fileNames) {
            builder.append(fileName);
        }
        mFileName =  builder.toString();

        String ChangelogUri;
        String propertyChangelogUri = SystemProperties.get("dk.changelog.uri");
        String configChangelogUpdateUri = res.getString(R.string.conf_changelog_server_url_def);

        if (!TextUtils.isEmpty(propertyChangelogUri)) {
            ChangelogUri = propertyChangelogUri;
        } else {
            ChangelogUri = configChangelogUpdateUri;
        }

        String channel = "release";

        if (mFileName != null) {
            if (mFileName.toLowerCase().contains("beta")) {
                channel = "beta";
            }

            final String finalChangelogUrl = ChangelogUri + channel + "/" + mFileName;
            fetchChangeLog(finalChangelogUrl);
        }
	    return null;
	}

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.change_log_dialog, null);
        final View progressContainer = view.findViewById(R.id.progress);
        mChangeLogView =
                (NotifyingWebView) view.findViewById(R.id.changelog);

        mChangeLogView.setOnInitialContentReadyListener(
                new NotifyingWebView.OnInitialContentReadyListener() {
                    @Override
                    public void onInitialContentReady(WebView webView) {
                        progressContainer.setVisibility(View.GONE);
                        mChangeLogView.setVisibility(View.VISIBLE);
                    }
                });

        mChangeLogView.getSettings().setTextZoom(80);
        mChangeLogView.setBackgroundColor(
                mContext.getResources().getColor(android.R.color.darker_gray));

        // Prepare the dialog box
        mAlertDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.changelog_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.dialog_close, null)
                .create();
        mAlertDialog.setOnDismissListener(this);
        mAlertDialog.show();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        File changeLog = new File(mContext.getCacheDir(), mFileName);

        if (changeLog.length() == 0) {
            // Change log is empty
            Toast.makeText(mContext, R.string.no_changelog_alert, Toast.LENGTH_SHORT).show();
        } else {
            // Load the url
            mChangeLogView.loadUrl(Uri.fromFile(changeLog).toString());
        }
    }

    private void fetchChangeLog(final String url) {
	    try {
		
		    URL changelogUrl = new URL(url);
		    URLConnection connection = changelogUrl.openConnection();
		    connection.connect();

		    InputStream input = new BufferedInputStream(changelogUrl.openStream());
		    OutputStream output = new FileOutputStream(mContext.getCacheDir() + "/" + mFileName);

		    byte data[] = new byte[1024];
		    int count;
		    try {
			    while ((count = input.read(data)) != -1)
				    output.write(data, 0, count);
		    } catch (IOException e) {
			    e.printStackTrace();
		    }

		    output.flush();
		    output.close();
		    input.close();
		
	    } catch (IOException ioe) {
		    ioe.printStackTrace();
	    }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        // Clean up
        mChangeLogView.destroy();
        mChangeLogView = null;
        mAlertDialog = null;
    }
}

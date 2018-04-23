/*
 * Copyright (C) 2018 OBONO
 * http://d.hatena.ne.jp/OBONO/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.obnsoft.arduboyemu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.obnsoft.arduboyemu.MyAsyncTaskWithDialog.Result;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {

    public static interface CancelCallback {
        boolean isCencelled(long length);
    }

    public static interface ResultHandler {
        void handleResult(Result result, File file);
    }

    /*-----------------------------------------------------------------------*/

    private static final String SCHEME_FILE     = "file";
    private static final String SCHEME_CONTENT  = "content";
    private static final String SCHEME_ARDUBOY  = "arduboy";

    private static final int BUFFER_SIZE = 1024 * 1024; // 1MiB

    public static void showCustomDialog(
            Context context, int iconId, int titleId, View view, final OnClickListener listener) {
        final AlertDialog dlg = new AlertDialog.Builder(context)
                .setIcon(iconId)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                    context.getText(android.R.string.cancel), (OnClickListener) null);
        }
        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dlg.getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
            if (listener != null) {
                editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            listener.onClick(dlg, AlertDialog.BUTTON_POSITIVE);
                            return true;
                        }
                        return false;
                    }
                });
            }
        }
        dlg.show();
    }

    public static void showMessageDialog(
            Context context, int iconId, int titleId, int messageId, OnClickListener listener) {
        AlertDialog dlg = new AlertDialog.Builder(context)
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, listener)
                .create();
        if (titleId != 0) {
            dlg.setTitle(titleId);
            if (iconId != 0) {
                dlg.setIcon(iconId);
            }
        }
        if (listener != null) {
            dlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                    context.getText(android.R.string.cancel), (OnClickListener) null);
        }
        dlg.show();
    }

    public static void showToast(Context context, int msgId) {
        Toast.makeText(context, msgId, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /*-----------------------------------------------------------------------*/

    public static String getParentPath(String filePath) {
        return new File(filePath).getParent();
    }

    public static String getBaseFileName(String filePath) {
        String fileName = new File(filePath).getName();
        int index = fileName.lastIndexOf('.');
        return (index >= 0) ? fileName.substring(0, index) : fileName;
    }

    public static long transferBytes(InputStream in, OutputStream out, CancelCallback callback)
            throws IOException {
        byte[]  buffer = new byte[BUFFER_SIZE];
        long    length = 0;
        int     readLength;
        while (!(callback != null && callback.isCencelled(length))
                && (readLength = in.read(buffer)) >= 0) {
            out.write(buffer, 0, readLength);
            length += readLength;
        }
        out.close(); 
        in.close();
        return length;
    }

    public static void downloadFile(final Context context, Uri uri, final ResultHandler handler) {
        final Uri actualUri;
        final boolean isNet;
        if (SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            if (handler != null) {
                handler.handleResult(Result.SUCCEEDED, new File(uri.getPath()));
            }
            return;
        } else if (SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
            actualUri = uri;
            isNet = false;
        } else if (SCHEME_ARDUBOY.equalsIgnoreCase(uri.getScheme())) {
            actualUri = Uri.parse(uri.getEncodedSchemeSpecificPart());
            isNet = true;
        } else {
            actualUri = uri;
            isNet = true;
        }

        String fileName = actualUri.getLastPathSegment().replaceAll("[\\\\/:*?\"<>|]", "_");
        final File file = generateTempFile(context, fileName);
        MyAsyncTaskWithDialog.ITask task = new MyAsyncTaskWithDialog.ITask() {
            private boolean mIsCancelled = false;
            @Override
            public Boolean task(ProgressDialog dialog) {
                try {
                    InputStream in;
                    if (isNet) {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpResponse httpResponse = httpclient
                                .execute(new HttpGet(actualUri.toString()));
                        in = httpResponse.getEntity().getContent();
                    } else {
                        in = context.getContentResolver().openInputStream(actualUri);
                    }
                    transferBytes(in, new FileOutputStream(file), new CancelCallback() {
                        @Override
                        public boolean isCencelled(long length) {
                            return mIsCancelled;
                        }
                    });
                } catch (Exception e){
                    e.printStackTrace();
                    file.delete();
                    return false;
                }
                return true;
            };
            @Override
            public void cancel() {
                mIsCancelled = true;
            }
            @Override
            public void post(Result result) {
                if (handler != null) {
                    handler.handleResult(result, file);
                }
            }
        };
        MyAsyncTaskWithDialog.execute(context, true, R.string.messageDownloading, task);
    }

    public static File generateTempFile(final Context context, String fileName) {
        return new File(context.getCacheDir(), fileName);
    }

    public static void cleanCacheFiles(Context context) {
        for (File file : context.getCacheDir().listFiles()) {
            file.delete();
        }
    }

    /*-----------------------------------------------------------------------*/

    public static void showVersion(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View aboutView = inflater.inflate(R.layout.about, new ScrollView(context));
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            TextView textView = (TextView) aboutView.findViewById(R.id.textAboutVersion);
            String versionStr = "Version ".concat(packageInfo.versionName);
            if (BuildConfig.DEBUG) {
                versionStr = versionStr.concat(" debug");
            }
            textView.setText(versionStr);

            StringBuilder buf = new StringBuilder();
            InputStream in = context.getResources().openRawResource(R.raw.license);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String str;
            while((str = reader.readLine()) != null) {
                buf.append(str).append('\n');
            }
            textView = (TextView) aboutView.findViewById(R.id.textAboutMessage);
            textView.setText(buf.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_menu_info_details)
                .setTitle(R.string.prefsAbout)
                .setView(aboutView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}

package com.obnsoft.arduboyemu;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

public class MyAsyncTaskWithDialog extends AsyncTask<Void, Integer, Boolean> {

    public static enum Result {
        SUCCEEDED, FAILED, CANCELLED
    }

    public interface ITask {
        public Boolean  task(ProgressDialog dialog);
        public void     cancel();
        public void     post(Result result);
    }

    public static void execute(Context context, boolean isSpinner, int messageId, ITask task) {
        (new MyAsyncTaskWithDialog(context, isSpinner, messageId, task)).execute();
    }

    /*-----------------------------------------------------------------------*/

    private Context         mContext;
    private boolean         mIsSpinner;
    private int             mMessageId;
    private ITask           mTask;
    private ProgressDialog  mDialog;

    private MyAsyncTaskWithDialog(Context context, boolean isSpinner, int messageId, ITask task) {
        mContext = context;
        mIsSpinner = isSpinner;
        mMessageId = messageId;
        mTask = task;
    }

    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setMessage(mContext.getText(mMessageId));
        if (mIsSpinner) {
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        } else {
            mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mDialog.setIndeterminate(true);
        }
        mDialog.setCancelable(false);
        mDialog.setButton(AlertDialog.BUTTON_NEUTRAL, mContext.getText(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mTask.cancel();
                        cancel(true);
                    }
        });
        mDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return mTask.task(mDialog);
    }

    @Override
    protected void onCancelled() {
        mDialog.dismiss();
        mTask.post(Result.CANCELLED);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mDialog.dismiss();
        mTask.post(result ? Result.SUCCEEDED : Result.FAILED);
    }


}

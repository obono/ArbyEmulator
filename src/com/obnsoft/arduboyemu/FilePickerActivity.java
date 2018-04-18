/*
 * Arduboy emulator using simavr on Android platform.
 *
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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class FilePickerActivity extends ListActivity {

    public static final String INTENT_EXTRA_DIRECTORY = "directory";
    public static final String INTENT_EXTRA_TOPDIRECTORY = "topDirectory";
    public static final String INTENT_EXTRA_EXTENSIONS = "extensions";
    public static final String INTENT_EXTRA_WRITEMODE = "writeMode";
    public static final String INTENT_EXTRA_SELECTPATH = "selectPath";

    public static final String EXT_ARDUBOY = ".arduboy";
    public static final String EXT_HEX = ".hex";
    public static final String EXT_EEPROM = ".eeprom";

    public static final String[] EXTS_FLASH = new String[] { EXT_HEX, EXT_ARDUBOY };
    public static final String[] EXTS_EEPROM = new String[] { EXT_EEPROM };

    private String mDirTop;
    private String mDirCurrent;
    private String[] mExtensions;
    private boolean mWriteMode = false;
    private int mPosNewEntry;
    private ArrayList<String> mStackPath = new ArrayList<String>();
    private FilePickerAdapter mAdapter;

    /*-----------------------------------------------------------------------*/

    class FilePickerAdapter extends ArrayAdapter<File> {

        private Context mContext;

        class FilePickerViewHolder {
            public ImageView    imageView;
            public TextView     textView;
        }

        public FilePickerAdapter(Context context) {
            super(context, 0);
            mContext = context;
        }

        public void setTargetDirectory(String path) {
            clear();
            File dir = new File(path);
            if (dir != null) {
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if (file.isHidden()) {
                            return false;
                        } else if (file.isDirectory()) {
                            return true;
                        }
                        return isExtensionMatched(file);
                    }
                });
                if (files != null) {
                    for (File file : files) {
                        add(file);
                    }
                    sort(new Comparator<File>() {
                        @Override
                        public int compare(File a, File b) {
                            if (a.isDirectory() != b.isDirectory()) {
                                return (a.isDirectory()) ? -1 : 1;
                            }
                            return a.getName().compareToIgnoreCase(b.getName());
                        }
                    });
                }
            }
            if (mWriteMode) {
                mPosNewEntry = getCount();
                add(null);
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FilePickerViewHolder holder;
            if (convertView == null) {
                LinearLayout ll = new LinearLayout(mContext);
                ll.setGravity(Gravity.CENTER_VERTICAL);
                holder = new FilePickerViewHolder();
                holder.imageView = new ImageView(mContext);
                holder.textView = new TextView(mContext);
                ll.addView(holder.imageView);
                ll.addView(holder.textView);
                ll.setTag(holder);
                convertView = ll;
            } else {
                holder = (FilePickerViewHolder) convertView.getTag();
            }
            File file = (File) getItem(position);
            holder.textView.setSingleLine(true);
            holder.textView.setTextAppearance(mContext, android.R.style.TextAppearance_Large);
            if (mWriteMode && position == mPosNewEntry) {
                holder.textView.setText(R.string.messageNewFile);
                holder.imageView.setImageResource(R.drawable.ic_item_file_new);
            } else {
                String fileName = file.getName();
                holder.textView.setText(fileName);
                int iconId = 0;
                if (file.isDirectory()) {
                    iconId = R.drawable.ic_item_folder;
                } else {
                    iconId = getIconIdFromFileName(fileName);
                }
                holder.imageView.setImageResource(iconId);
            }
            return convertView;
        }
    }

    /*-----------------------------------------------------------------------*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.file_picker_activity);

        Intent intent = getIntent();
        String path = null;
        if (intent != null) {
            path = intent.getStringExtra(INTENT_EXTRA_DIRECTORY);
            mDirTop = intent.getStringExtra(INTENT_EXTRA_TOPDIRECTORY);
            mExtensions = intent.getStringArrayExtra(INTENT_EXTRA_EXTENSIONS);
            mWriteMode = intent.getBooleanExtra(INTENT_EXTRA_WRITEMODE, false);
        }
        setTitle(mWriteMode ? R.string.appNameFilePickerWrite : R.string.appNameFilePickerRead);

        /*  Check top directory.  */
        if (mDirTop == null) {
            mDirTop = Environment.getExternalStorageDirectory().getPath();
        }
        if (!mDirTop.endsWith(File.separator)) {
            mDirTop += File.separator;
        }

        /*  Check current directory.  */
        if (path == null) {
            path = mDirTop;
        } else {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            if (!path.startsWith(mDirTop)) {
                path = mDirTop;
            }
        }

        /*  Check extension.  */
        if (mExtensions != null) {
            for (int i = 0; i < mExtensions.length; i++) {
                mExtensions[i] = tuneExtension(mExtensions[i]);
            }
        }

        mAdapter = new FilePickerAdapter(this);
        setListAdapter(mAdapter);
        setCurrentDirectory(path);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        File file = (File) mAdapter.getItem(position);
        if (mWriteMode && position == mPosNewEntry) {
            onNewFileRequested(mDirCurrent, (mExtensions != null) ? mExtensions[0] : null);
        } else if (file.isDirectory()) {
            mStackPath.add(mDirCurrent);
            setCurrentDirectory(file.getPath().concat(File.separator));
        } else {
            onFileSelected(file.getPath());
        }
    }

    @Override
    public void onBackPressed() {
        String path = getLastDirectory();
        if (path == null) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        } else {
            mStackPath.remove(mStackPath.size() - 1);
            setCurrentDirectory(path);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_picker, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.menuFilePickerBack);
        item.setVisible(!mStackPath.isEmpty());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        case R.id.menuFilePickerBack:
            onBackPressed();
            return true;
        case R.id.menuFilePickerGoUpper:
            goToUpperDirectory();
            return true;
        }
        return false;
    }

    /*-----------------------------------------------------------------------*/

    public void setCurrentDirectory(String path) {
        mDirCurrent = path;
        mAdapter.setTargetDirectory(path);
        getListView().smoothScrollBy(0, 0); // Stop momentum scrolling
        onCurrentDirectoryChanged(path);
    }

    public void onCurrentDirectoryChanged(String path) {
        TextView tv = (TextView) findViewById(R.id.textViewCurrentDirectory);
        tv.setText(getTrimmedCurrentDirectory(path));
        invalidateOptionsMenu();
    }

    public void onFileSelected(String path) {
        setResultAndFinish(path);
    }

    public void onNewFileRequested(final String directory, final String extension) {
        String fileName = getString(R.string.fileName);
        int fileNameLen = fileName.length();
        if (extension != null) {
            fileName = fileName.concat(extension);
        }
        final EditText editText = new EditText(this);
        editText.setSingleLine();
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        editText.setText(fileName);
        editText.setSelection(0, fileNameLen);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String fileName = editText.getText().toString().trim();
                if (fileName.length() == 0 || fileName.startsWith(".") ||
                        fileName.contains(File.separator) || fileName.contains(File.pathSeparator)) {
                    Utils.showToast(FilePickerActivity.this, R.string.messageInvalid);
                    return;
                }
                String newPath = directory.concat(fileName);
                if (extension != null && !newPath.endsWith(extension)) {
                    newPath = newPath.concat(extension);
                }
                if (new File(newPath).exists()) {
                    onFileSelected(newPath);
                } else {
                    setResultAndFinish(newPath);
                }
            }
        };
        Utils.showCustomDialog(this, R.drawable.ic_item_file_new,
                R.string.messageNewFile, editText, listener);
    }

    public void goToUpperDirectory() {
        String path = getUpperDirectory();
        if (path != null) {
            mStackPath.add(mDirCurrent);
            setCurrentDirectory(path);
        }
    }

    public String getTopDirectory() {
        return mDirTop;
    }

    public String getCurrentDirectory() {
        return mDirCurrent;
    }

    public String[] getExtensions() {
        return mExtensions;
    }

    public boolean isWriteMode() {
        return mWriteMode;
    }

    public String getTrimmedCurrentDirectory(String path) {
        if (path != null && path.startsWith(mDirTop)) {
            return path.substring(mDirTop.length());
        }
        return null;
    }

    public String getLastDirectory() {
        int size = mStackPath.size();
        return (size > 0) ? mStackPath.get(size - 1) : null;
    }

    public String getUpperDirectory() {
        if (mDirCurrent.equals(mDirTop)) {
            return null;
        }
        return new File(mDirCurrent).getParent().concat(File.separator);
    }

    public void setResultAndFinish(String path) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_SELECTPATH, path);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static int getIconIdFromFileName(String fileName) {
        int ret = 0;
        int index = fileName.lastIndexOf('.');
        if (index >= 0) {
            String extension =
                    fileName.substring(index).toLowerCase(Locale.getDefault());
            if (EXT_ARDUBOY.equals(extension)) {
                ret = R.drawable.ic_item_file_arduboy;
            } else if (EXT_HEX.equals(extension)) {
                ret = R.drawable.ic_item_file_hex;
            } else if (EXT_EEPROM.equals(extension)) {
                ret = R.drawable.ic_item_file_eeprom;
            }
        }
        return ret;
    }

    /*-----------------------------------------------------------------------*/

    private String tuneExtension(String extension) {
        if (extension != null) {
            extension = extension.toLowerCase(Locale.getDefault());
            if (!extension.startsWith(".")) {
                extension = ".".concat(extension);
            }
        }
        return extension;
    }

    private boolean isExtensionMatched(File file) {
        if (mExtensions == null) {
            return true;
        }
        String name = file.getName().toLowerCase(Locale.getDefault());
        for (String extension : mExtensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}

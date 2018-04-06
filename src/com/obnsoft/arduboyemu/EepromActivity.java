package com.obnsoft.arduboyemu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class EepromActivity extends Activity {

    private static final int EEPROM_SIZE = 1024;
    private static final int COLUMNS_PER_LINE = 8;
    private static final int REQUEST_RESTORE_EEPROM = 2;
    private static final int REQUEST_BACKUP_EEPROM = 3;

    private TextView mTextViewHeader;
    private TextView mTextViewBody;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eeprom_activity);
        mTextViewHeader = (TextView) findViewById(R.id.textViewEepromHeader);
        mTextViewBody = (TextView) findViewById(R.id.textViewEepromBody);

        StringBuffer buf = new StringBuffer("    ");
        for (int col = 0; col < COLUMNS_PER_LINE; col++) {
            buf.append(String.format(" +%X", col));
        }
        mTextViewHeader.setText(buf.toString());

        SpannableStringBuilder sb = new SpannableStringBuilder();  
        byte eeprom[] = new byte[EEPROM_SIZE]; //Native.getEEPROM();
        for (int row = 0; row < EEPROM_SIZE / COLUMNS_PER_LINE; row++) {
            int start = sb.length();
            ForegroundColorSpan span = new ForegroundColorSpan(Color.GRAY);
            sb.append(String.format("%03X:", row * COLUMNS_PER_LINE));
            sb.setSpan(span, start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
            for (int col = 0; col < COLUMNS_PER_LINE; col++) {
                sb.append(String.format(" %02X", eeprom[row * COLUMNS_PER_LINE + col]));
            }
            sb.append('\n');
        }
        int length = sb.length();
        sb.delete(length - 1, length);
        mTextViewBody.setText(sb);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.eeprom, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.INTENT_EXTRA_EXTENSIONS, new String[] {"eeprom"});
        intent.putExtra(FilePickerActivity.INTENT_EXTRA_WRITEMODE, false);
        intent.putExtra(FilePickerActivity.INTENT_EXTRA_DIRECTORY,
                SettingsActivity.getPathEeprom(this));
        switch (item.getItemId()) {
        case R.id.menuEepromRestore:
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_WRITEMODE, false);
            startActivityForResult(intent, REQUEST_RESTORE_EEPROM);
            return true;
        case R.id.menuEepromBackup:
            intent.putExtra(FilePickerActivity.INTENT_EXTRA_WRITEMODE, true);
            startActivityForResult(intent, REQUEST_BACKUP_EEPROM);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_RESTORE_EEPROM:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FilePickerActivity.INTENT_EXTRA_SELECTPATH);
                // TODO
                SettingsActivity.setPathEeprom(this, path);
            }
            break;
        case REQUEST_BACKUP_EEPROM:
            if (resultCode == RESULT_OK) {
                String path = data.getStringExtra(FilePickerActivity.INTENT_EXTRA_SELECTPATH);
                // TODO
                SettingsActivity.setPathEeprom(this, path);
            }
            break;
        }
    }

}

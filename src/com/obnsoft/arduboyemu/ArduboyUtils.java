package com.obnsoft.arduboyemu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArduboyUtils {

    private static final String UTF8 = "UTF-8";
    private static final String INFO_FILE_NAME = "info.json";
    private static final String JSON_KEY_BINARIES = "binaries";
    private static final String JSON_KEY_FILENAME = "filename";

    public static boolean extractHexFromArduboy(File arduboyFile, File outFile) {
        try {
            InputStream in = extractStreamFileFromZip(arduboyFile, INFO_FILE_NAME);
            if (in == null) {
                return false;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Utils.transferBytes(in, out, null);
            JSONObject infoJson = new JSONObject(new String(out.toByteArray(), UTF8));
            JSONArray binariesJson = infoJson.getJSONArray(JSON_KEY_BINARIES);
            JSONObject binaryJson = (JSONObject) binariesJson.get(0);
            String hexFileName = binaryJson.getString(JSON_KEY_FILENAME);
            in = extractStreamFileFromZip(arduboyFile, hexFileName);
            if (in == null) {
                return false;
            }
            Utils.transferBytes(in, new FileOutputStream(outFile), null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static InputStream extractStreamFileFromZip(File zipFile, String fname) {
        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(new FileInputStream(zipFile));
            for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
                if (entry.getName().equals(fname)) {
                    return zin;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e2) {
                    // do nothing
                }
            }
        }
        return null;
    }
}

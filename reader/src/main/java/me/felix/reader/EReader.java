package me.felix.reader;

import android.content.Context;
import android.text.TextUtils;

import org.geometerplus.android.fbreader.api.FBReaderIntents;

import java.io.File;

public class EReader {

    public static boolean open(Context context, String path) {
        if (TextUtils.isEmpty(path)) return false;
        if (!new File(path).exists()) return false;
        try {
            String lowCasePath = path.toLowerCase();
            if (lowCasePath.endsWith(".txt")
                    || lowCasePath.endsWith(".epub")
                    || lowCasePath.endsWith(".mobi")) {
                FBReaderIntents.startReadBook(context, path);
            } else if (lowCasePath.endsWith(".pdf")) {
                context.startActivity(PdfActivity.newIntent(context, path));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

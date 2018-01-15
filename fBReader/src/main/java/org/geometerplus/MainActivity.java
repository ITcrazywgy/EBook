package org.geometerplus;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.zlibrary.ui.android.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            String path = Environment.getExternalStorageDirectory() + "/js.epub";
            FBReaderIntents.startReadBook(this, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

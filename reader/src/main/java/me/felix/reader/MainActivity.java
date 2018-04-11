package me.felix.reader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EReader.open(this, Environment.getExternalStorageDirectory() + "/OpenGL.pdf");
        EReader.open(this, Environment.getExternalStorageDirectory() + "/book.txt");
        finish();
    }
}

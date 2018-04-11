package me.felix.reader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;

import java.io.File;

public class PdfActivity extends Activity {

    private String filePath;
    private LinearLayout mNavigationPanel;
    private PDFView mPdfView;

    public static Intent newIntent(Context context, String path) {
        Intent intent = new Intent(context, PdfActivity.class);
        intent.putExtra("path", path);
        return intent;
    }

    private SharedPreferences sp;

    private SharedPreferences getSharedPreferences() {
        if (sp == null) {
            sp = getSharedPreferences("books_progress", Context.MODE_PRIVATE);
        }
        return sp;
    }

    @SuppressLint("DefaultLocale")
    private void saveProgress(int page, int pageCount) {
        getSharedPreferences().edit().putString(filePath, String.format("%d,%d", page, pageCount)).apply();
    }


    private int mCurrentPage = -1;
    private int mLastPage = -1;
    private int mPageCount = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        initNavigationPanel();

        filePath = getIntent().getStringExtra("path");
        mPdfView = (PDFView) findViewById(R.id.pdfView);
        File file = new File(filePath);
        if (file.exists()) {
            String progress = getSharedPreferences().getString(filePath, "0,0");
            int lastPage = Integer.parseInt(progress.split(",")[0]);
            mPdfView.fromFile(file)
                    .onPageChange(new OnPageChangeListener() {
                        @Override
                        public void onPageChanged(int page, int pageCount) {
                            saveProgress(page, pageCount);
                            PdfActivity.this.onPageChanged(page, pageCount, false);
                        }
                    })
                    .onTap(new OnTapListener() {
                        @Override
                        public boolean onTap(MotionEvent e) {
                            toggleNavigationPanel();
                            return true;
                        }
                    })
                    .defaultPage(lastPage)
                    .load();
        }
    }

    private void toggleNavigationPanel() {
        if (mCurrentPage == -1 || mPageCount == -1) return;
        if (mNavigationPanel.getVisibility() == View.VISIBLE) {
            hideNavigationPanel();
        } else {
            mNavigationPanel.setVisibility(View.VISIBLE);
            setupNavigationProgress(mCurrentPage, mPageCount);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                prePage();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                nextPage();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void nextPage() {
        if (mCurrentPage < mPageCount - 1) {
            mPdfView.jumpTo(++mCurrentPage);
        }
    }

    private void prePage() {
        if (mCurrentPage > 0) {
            mPdfView.jumpTo(--mCurrentPage);
        }
    }


    private void onPageChanged(int page, int pageCount, boolean fromSeekBar) {
        if (!isTrackingTouch) {
            mLastPage = mCurrentPage;
        }
        mCurrentPage = page;
        setupNavigationProgress(page, pageCount);
        mPageCount = pageCount;
        if (fromSeekBar) {
            mPdfView.jumpTo(page);
        }
    }

    private boolean isTrackingTouch = false;

    private void initNavigationPanel() {
        mNavigationPanel = (LinearLayout) findViewById(R.id.navigation_panel);
        SeekBar mSlider = (SeekBar) mNavigationPanel.findViewById(R.id.navigation_slider);
        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    onPageChanged(progress, seekBar.getMax(), true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = true;
                mLastPage = mPdfView.getCurrentPage();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingTouch = false;
            }
        });
        final Button btnOk = (Button) mNavigationPanel.findViewById(R.id.navigation_ok);
        final Button btnCancel = (Button) mNavigationPanel.findViewById(R.id.navigation_cancel);
        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View v) {
                if (v.getId() == R.id.navigation_cancel) {
                    if (mLastPage != mCurrentPage) {
                        mPdfView.jumpTo(mLastPage);
                        mLastPage = mCurrentPage;
                    }
                }
                hideNavigationPanel();
            }
        };
        btnOk.setOnClickListener(listener);
        btnCancel.setOnClickListener(listener);
        btnOk.setText("确定");
        btnCancel.setText("取消");
    }

    @SuppressLint("SetTextI18n")
    private void setupNavigationProgress(int page, int pageCount) {
        final SeekBar slider = (SeekBar) findViewById(R.id.navigation_slider);
        final TextView text = (TextView) findViewById(R.id.navigation_text);
        if (slider.getMax() != (pageCount - 1) || slider.getProgress() != page) {
            slider.setMax(pageCount - 1);
            slider.setProgress(page);
        }
        text.setText(String.valueOf(page + 1) + "/" + pageCount);
    }


    private void hideNavigationPanel() {
        mNavigationPanel.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (mNavigationPanel.getVisibility() == View.VISIBLE) {
            hideNavigationPanel();
        } else {
            super.onBackPressed();
        }
    }
}

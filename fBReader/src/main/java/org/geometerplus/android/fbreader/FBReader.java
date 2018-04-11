/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.android.util.UIUtil;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookCollection;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.zlibrary.core.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.core.view.ZLViewWidget;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.ui.android.R;
import org.geometerplus.zlibrary.ui.android.view.AndroidFontUtil;
import org.geometerplus.zlibrary.ui.android.view.ZLAndroidWidget;

public final class FBReader extends FBReaderMainActivity implements ZLApplicationWindow {

    private FBReaderApp myFBReaderApp;
    private volatile Book myBook;
    private RelativeLayout myRootView;
    private ZLAndroidWidget myMainView;

    private synchronized void openBook(Intent intent, boolean force) {
        if (!force && myBook != null) {
            return;
        }
        try {
            myBook = new Book(FBReaderIntents.getBookExtra(intent), Paths.systemInfo(this));
        } catch (BookReadingException e) {
            e.printStackTrace();
        }
        if (myBook != null) {
            ZLFile file = BookUtil.fileByBook(myBook);
            if (!file.exists()) {
                myBook = null;
            }
        }
        myFBReaderApp.openBook(myBook, null);
        AndroidFontUtil.clearFontCache();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        myRootView = (RelativeLayout) findViewById(R.id.root_view);
        myMainView = (ZLAndroidWidget) findViewById(R.id.main_view);
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        myFBReaderApp = (FBReaderApp) FBReaderApp.Instance();
        if (myFBReaderApp == null) {
            SystemInfo systemInfo = Paths.systemInfo(this);
            myFBReaderApp = new FBReaderApp(systemInfo, new BookCollection(systemInfo, this));
        }

        myBook = null;
        myFBReaderApp.setWindow(this);
        myFBReaderApp.initWindow();

        //选中文字弹框
        if (myFBReaderApp.getPopupById(SelectionPopup.ID) == null) {
            new SelectionPopup(myFBReaderApp);
        }
        //快速翻看弹框
        if (myFBReaderApp.getPopupById(NavigationPopup.ID) == null) {
            new NavigationPopup(myFBReaderApp);
        }

        //处理超链接
        myFBReaderApp.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, myFBReaderApp));
        myFBReaderApp.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, myFBReaderApp));

        //快速翻看
        myFBReaderApp.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, myFBReaderApp));

        openBook(getIntent(), true);
    }

    public void navigate() {
        ((NavigationPopup)myFBReaderApp.getPopupById(NavigationPopup.ID)).runNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((PopupPanel) myFBReaderApp.getPopupById(SelectionPopup.ID)).setPanelInfo(this, myRootView);
        ((NavigationPopup)myFBReaderApp.getPopupById(NavigationPopup.ID)).setPanelInfo(this, myRootView);
    }


    public void showSelectionPanel() {
        final ZLTextView view = myFBReaderApp.getTextView();
        ((SelectionPopup) myFBReaderApp.getPopupById(SelectionPopup.ID)).move(view.getSelectionStartY(), view.getSelectionEndY());
        myFBReaderApp.showPopup(SelectionPopup.ID);
    }

    public void hideSelectionPanel() {
        final FBReaderApp.PopupPanel popup = myFBReaderApp.getActivePopup();
        if (popup != null && popup.getId() == SelectionPopup.ID) {
            myFBReaderApp.hideActivePopup();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        myStartTimer = true;
        registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(myBatteryInfoReceiver);
        } catch (IllegalArgumentException e) {
            // do nothing, this exception means that myBatteryInfoReceiver was not registered
        }

        myFBReaderApp.stopTimer();
        if (getZLibrary().DisableButtonLightsOption.getValue()) {
            setButtonLight(true);
        }
        myFBReaderApp.onWindowClosing();
        super.onPause();
    }

    private void setButtonLight(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            setButtonLightInternal(enabled);
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void setButtonLightInternal(boolean enabled) {
        final WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.buttonBrightness = enabled ? -1.0f : 0.0f;
        getWindow().setAttributes(attrs);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PopupPanel.removeAllWindows(myFBReaderApp, this);
    }

    @Override
    public void onLowMemory() {
        myFBReaderApp.onWindowClosing();
        super.onLowMemory();
    }

    @Override
    public boolean onSearchRequested() {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return (myMainView != null && myMainView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
    }


    @Override
    public FBReaderApp.SynchronousExecutor createExecutor(String key) {
        return UIUtil.createExecutor(this, key);
    }


    private int myBatteryLevel;

    @Override
    public int getBatteryLevel() {
        return myBatteryLevel;
    }


    @Override
    public void close() {
        finish();
    }

    @Override
    public ZLViewWidget getViewWidget() {
        return myMainView;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();

    }

    @Override
    public void setWindowTitle(final String title) {
        runOnUiThread(new Runnable() {
            public void run() {
                setTitle(title);
            }
        });
    }

    @Override
    public void showErrorMessage(String resourceKey) {

    }

    @Override
    public void showErrorMessage(String resourceKey, String parameter) {

    }


    private PowerManager.WakeLock myWakeLock;
    private boolean myWakeLockToCreate;
    private boolean myStartTimer;

    public final void createWakeLock() {
        if (myWakeLockToCreate) {
            synchronized (this) {
                if (myWakeLockToCreate) {
                    myWakeLockToCreate = false;
                    myWakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "FBReader");
                    myWakeLock.acquire();
                }
            }
        }
        if (myStartTimer) {
            myFBReaderApp.startTimer();
            myStartTimer = false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        switchWakeLock(hasFocus &&
                getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() <
                        myFBReaderApp.getBatteryLevel()
        );
    }

    private final void switchWakeLock(boolean on) {
        if (on) {
            if (myWakeLock == null) {
                myWakeLockToCreate = true;
            }
        } else {
            if (myWakeLock != null) {
                synchronized (this) {
                    if (myWakeLock != null) {
                        myWakeLock.release();
                        myWakeLock = null;
                    }
                }
            }
        }
    }

    private void setBatteryLevel(int percent) {
        myBatteryLevel = percent;
    }

    private BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final int level = intent.getIntExtra("level", 100);
            setBatteryLevel(level);
            switchWakeLock(
                    hasWindowFocus() &&
                            getZLibrary().BatteryLevelToTurnScreenOffOption.getValue() < level
            );
        }
    };
}

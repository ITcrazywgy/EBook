/*
 * Copyright (C) 2007-2015 FBReader.ORG Limited <contact@fbreader.org>
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

package org.geometerplus.fbreader.fbreader;

import org.geometerplus.fbreader.book.Author;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookUtil;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.options.ImageOptions;
import org.geometerplus.fbreader.fbreader.options.MiscOptions;
import org.geometerplus.fbreader.fbreader.options.PageTurningOptions;
import org.geometerplus.fbreader.fbreader.options.ViewOptions;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.fbreader.util.AutoTextSnippet;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.application.ZLKeyBindings;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextParagraphCursor;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.text.view.ZLTextWordCursor;

import java.util.Date;
import java.util.HashMap;

public final class FBReaderApp extends ZLApplication {


    public final MiscOptions MiscOptions = new MiscOptions();
    public final ImageOptions ImageOptions = new ImageOptions();
    public final org.geometerplus.fbreader.fbreader.options.ViewOptions ViewOptions = new ViewOptions();
    public final PageTurningOptions PageTurningOptions = new PageTurningOptions();

    private final ZLKeyBindings myBindings = new ZLKeyBindings();

    public final FBView BookTextView;
    public final FBView FootnoteView;

    public volatile BookModel Model;
    public volatile Book ExternalBook;


    public FBReaderApp(SystemInfo systemInfo) {
        super(systemInfo);
        //文字放大、缩小
        addAction(ActionCode.INCREASE_FONT, new ChangeFontSizeAction(this, +2));
        addAction(ActionCode.DECREASE_FONT, new ChangeFontSizeAction(this, -2));

        //搜索后上下查找
        addAction(ActionCode.FIND_NEXT, new FindNextAction(this));
        addAction(ActionCode.FIND_PREVIOUS, new FindPreviousAction(this));
        addAction(ActionCode.CLEAR_FIND_RESULTS, new ClearFindResultsAction(this));

        //清除选中文字
        addAction(ActionCode.SELECTION_CLEAR, new SelectionClearAction(this));

        //翻页
        addAction(ActionCode.TURN_PAGE_FORWARD, new TurnPageAction(this, true));
        addAction(ActionCode.TURN_PAGE_BACK, new TurnPageAction(this, false));

        //框选文字
        addAction(ActionCode.MOVE_CURSOR_UP, new MoveCursorAction(this, FBView.Direction.up));
        addAction(ActionCode.MOVE_CURSOR_DOWN, new MoveCursorAction(this, FBView.Direction.down));
        addAction(ActionCode.MOVE_CURSOR_LEFT, new MoveCursorAction(this, FBView.Direction.rightToLeft));
        addAction(ActionCode.MOVE_CURSOR_RIGHT, new MoveCursorAction(this, FBView.Direction.leftToRight));

        //上下键翻页
        addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new VolumeKeyTurnPageAction(this, true));
        addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new VolumeKeyTurnPageAction(this, false));

        //退出
        addAction(ActionCode.EXIT, new ExitAction(this));

        BookTextView = new FBView(this);
        FootnoteView = new FBView(this);

        setView(BookTextView);
    }

    public Book getCurrentBook() {
        final BookModel m = Model;
        return m != null ? m.Book : ExternalBook;
    }

    private boolean isSameBook(Book b0, Book b1) {
        return b0 == b1 || !(b0 == null || b1 == null) && b0.getPath().equals(b1.getPath());
    }

    public void openBook(Book book, Runnable postAction) {
        if (book == null) {
            return;
        }
        if (Model != null && isSameBook(book, Model.Book)) {
            return;
        }

        final Book bookToOpen = book;
        bookToOpen.addNewLabel(Book.READ_LABEL);

        final SynchronousExecutor executor = createExecutor("loadingBook");
        executor.execute(new Runnable() {
            public void run() {
                openBookInternal(bookToOpen, false);
            }
        }, postAction);
    }

    private void reloadBook() {
        final Book book = getCurrentBook();
        if (book != null) {
            final SynchronousExecutor executor = createExecutor("loadingBook");
            executor.execute(new Runnable() {
                public void run() {
                    openBookInternal(book, true);
                }
            }, null);
        }
    }

    public ZLKeyBindings keyBindings() {
        return myBindings;
    }

    public FBView getTextView() {
        return (FBView) getCurrentView();
    }

    public AutoTextSnippet getFootnoteData(String id) {
        if (Model == null) {
            return null;
        }
        final BookModel.Label label = Model.getLabel(id);
        if (label == null) {
            return null;
        }
        final ZLTextModel model;
        if (label.ModelId != null) {
            model = Model.getFootnoteModel(label.ModelId);
        } else {
            model = Model.getTextModel();
        }
        if (model == null) {
            return null;
        }
        final ZLTextWordCursor cursor =
                new ZLTextWordCursor(new ZLTextParagraphCursor(model, label.ParagraphIndex));
        final AutoTextSnippet longSnippet = new AutoTextSnippet(cursor, 140);
        if (longSnippet.IsEndOfText) {
            return longSnippet;
        } else {
            return new AutoTextSnippet(cursor, 100);
        }
    }


    public void clearTextCaches() {
        BookTextView.clearCaches();
        FootnoteView.clearCaches();
    }


    private synchronized void openBookInternal(final Book book, boolean force) {
        if (!force && Model != null && isSameBook(book, Model.Book)) {
            return;
        }

        BookTextView.setModel(null);
        FootnoteView.setModel(null);
        clearTextCaches();
        Model = null;
        ExternalBook = null;
        System.gc();
        System.gc();

        final PluginCollection pluginCollection = PluginCollection.Instance(SystemInfo);
        final FormatPlugin plugin;
        try {
            plugin = BookUtil.getPlugin(pluginCollection, book);
        } catch (BookReadingException e) {
            processException(e);
            return;
        }

        try {
            //利用内置插件创建填充BookModel
            Model = BookModel.createModel(book, plugin);
            ZLTextHyphenator.Instance().load(book.getLanguage());
            BookTextView.setModel(Model.getTextModel());
            setView(BookTextView);
            final StringBuilder title = new StringBuilder(book.getTitle());
            if (!book.authors().isEmpty()) {
                boolean first = true;
                for (Author a : book.authors()) {
                    title.append(first ? " (" : ", ");
                    title.append(a.DisplayName);
                    first = false;
                }
                title.append(")");
            }
            setTitle(title.toString());
        } catch (BookReadingException e) {
            processException(e);
        }

        getViewWidget().reset();
        getViewWidget().repaint();
    }


    public void showBookTextView() {
        setView(BookTextView);
    }


    private ZLTextPosition myJumpEndPosition;
    private Date myJumpTimeStamp;
    public void tryOpenFootnote(String id) {
        if (Model != null) {
            myJumpEndPosition = null;
            myJumpTimeStamp = null;
            final BookModel.Label label = Model.getLabel(id);
            if (label != null) {
                if (label.ModelId == null) {
                    if (getTextView() == BookTextView) {
                        //addInvisibleBookmark();//用于返回到跳转前的位置
                        myJumpEndPosition = new ZLTextFixedPosition(label.ParagraphIndex, 0, 0);
                        myJumpTimeStamp = new Date();
                    }
                    BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
                    setView(BookTextView);
                } else {
                    setFootnoteModel(label.ModelId);
                    setView(FootnoteView);
                    FootnoteView.gotoPosition(label.ParagraphIndex, 0, 0);
                }
                getViewWidget().repaint();
                //storePosition();
            }
        }
    }

    private void setFootnoteModel(String modelId) {
        final ZLTextModel model = Model.getFootnoteModel(modelId);
        FootnoteView.setModel(model);
        if (model != null) {
            //myFootnoteModelId = modelId;
            //setBookmarkHighlightings(FootnoteView, modelId);
        }
    }



}

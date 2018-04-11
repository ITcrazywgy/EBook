package org.geometerplus.fbreader.book;

import android.content.Context;

import org.geometerplus.android.fbreader.SQLiteBooksDatabase;
import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.core.util.SystemInfo;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class BookCollection {

    public final org.geometerplus.fbreader.formats.PluginCollection PluginCollection;
    private final SQLiteBooksDatabase myDatabase;
    private final Map<ZLFile, Book> myBooksByFile = Collections.synchronizedMap(new LinkedHashMap<ZLFile, Book>());


    public BookCollection(SystemInfo systemInfo, Context context) {
        PluginCollection = org.geometerplus.fbreader.formats.PluginCollection.Instance(systemInfo);
        myDatabase = new SQLiteBooksDatabase(context);
    }

    public int size() {
        return myBooksByFile.size();
    }

    public Book getBookByFile(String path) {
        return getBookByFile(ZLFile.createFileByPath(path));
    }

    private Book getBookByFile(ZLFile bookFile) {
        if (bookFile == null) {
            return null;
        }
        return getBookByFile(bookFile, PluginCollection.getPlugin(bookFile));
    }

    private Book getBookByFile(ZLFile bookFile, final FormatPlugin plugin) {
        if (plugin == null) {
            return null;
        }
        try {
            bookFile = plugin.realBookFile(bookFile);
        } catch (BookReadingException e) {
            return null;
        }
        Book book = myBooksByFile.get(bookFile);
        if (book != null) {
            return book;
        }
        final ZLPhysicalFile physicalFile = bookFile.getPhysicalFile();
        if (physicalFile != null && !physicalFile.exists()) {
            return null;
        }
        book = myDatabase.loadBookByFile(bookFile.getPath());
        try {
            if (book != null) {
                BookUtil.readMetainfo(book, plugin);
            }
        } catch (BookReadingException e) {
            return null;
        }
        saveBook(book);
        return book;
    }


    public synchronized long saveBook(Book book) {
        Book dbBook = myDatabase.loadBookByFile(book.getPath());
        long id;
        if (dbBook != null && dbBook.getId() >= 0) {
            myDatabase.updateBookInfo(dbBook.getId(), book.getPath(), book.myEncoding, book.myLanguage, book.getTitle());
            id = dbBook.getId();
        } else {
            id = myDatabase.insertBookInfo(book.getPath(), book.myEncoding, book.myLanguage, book.getTitle());
        }
        if (book.getProgress() != null) {
            myDatabase.saveBookProgress(id, book.getProgress());
        }
        return id;
    }


    public void storePosition(long bookId, ZLTextPosition position) {
        if (bookId != -1) {
            myDatabase.storePosition(bookId, position);
        }
    }

    public ZLTextFixedPosition.WithTimestamp getStoredPosition(long bookId) {
        return myDatabase.getStoredPosition(bookId);
    }

}

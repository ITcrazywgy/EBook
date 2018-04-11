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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.geometerplus.android.util.SQLiteUtil;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.util.RationalNumber;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;

import java.util.HashMap;

public final class SQLiteBooksDatabase {
    private final SQLiteDatabase myDatabase;
    private final HashMap<String, SQLiteStatement> myStatements = new HashMap<>();

    public SQLiteBooksDatabase(Context context) {
        myDatabase = context.openOrCreateDatabase("books.db", Context.MODE_PRIVATE, null);
        migrate();
    }

    @Override
    public void finalize() {
        myDatabase.close();
    }

    protected void executeAsTransaction(Runnable actions) {
        boolean transactionStarted = false;
        try {
            myDatabase.beginTransaction();
            transactionStarted = true;
        } catch (Throwable t) {
        }
        try {
            actions.run();
            if (transactionStarted) {
                myDatabase.setTransactionSuccessful();
            }
        } finally {
            if (transactionStarted) {
                myDatabase.endTransaction();
            }
        }
    }

    private void migrate() {
        final int version = myDatabase.getVersion();
        final int currentVersion = 1;
        if (version >= currentVersion) {
            return;
        }
        myDatabase.beginTransaction();

        switch (version) {
            case 0:
                createTables();
        }
        myDatabase.setTransactionSuccessful();
        myDatabase.setVersion(currentVersion);
        myDatabase.endTransaction();
        myDatabase.execSQL("VACUUM");
    }


    private Book createBook(long id, String filePath, String title, String encoding, String language) {
        return new Book(id, filePath, title, encoding, language);
    }


    public Book loadBookByFile(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            return null;
        }
        Book book = null;
        final Cursor cursor = myDatabase.rawQuery("SELECT book_id,title,encoding,language FROM Books WHERE file_path=?" ,new String[]{filePath});
        if (cursor.moveToNext()) {
            book = createBook(cursor.getLong(0), filePath, cursor.getString(1), cursor.getString(2), cursor.getString(3));
        }
        cursor.close();
        return book;
    }


    public void updateBookInfo(long bookId, String filePath, String encoding, String language, String title) {
        final SQLiteStatement statement = get("UPDATE OR IGNORE Books SET file_path=?, encoding=?, language=?, title=? WHERE book_id=?");
        statement.bindString(1, filePath);
        SQLiteUtil.bindString(statement, 2, encoding);
        SQLiteUtil.bindString(statement, 3, language);
        statement.bindString(4, title);
        statement.bindLong(5, bookId);
        statement.execute();
    }

    public long insertBookInfo(String filePath, String encoding, String language, String title) {
        final SQLiteStatement statement = get(
                "INSERT OR IGNORE INTO Books (encoding,language,title,file_path) VALUES (?,?,?,?)"
        );
        SQLiteUtil.bindString(statement, 1, encoding);
        SQLiteUtil.bindString(statement, 2, language);
        statement.bindString(3, title);
        statement.bindString(4, filePath);
        return statement.executeInsert();
    }


    public ZLTextFixedPosition.WithTimestamp getStoredPosition(long bookId) {
        ZLTextFixedPosition.WithTimestamp position = null;
        final Cursor cursor = myDatabase.rawQuery("SELECT paragraph,word,char,timestamp FROM BookState WHERE book_id = " + bookId, null);
        if (cursor.moveToNext()) {
            position = new ZLTextFixedPosition.WithTimestamp(
                    (int) cursor.getLong(0),
                    (int) cursor.getLong(1),
                    (int) cursor.getLong(2),
                    cursor.getLong(3)
            );
        }
        cursor.close();
        return position;
    }

    public void storePosition(long bookId, ZLTextPosition position) {
        final SQLiteStatement statement = get(
                "INSERT OR REPLACE INTO BookState (book_id,paragraph,word,char,timestamp) VALUES (?,?,?,?,?)"
        );
        statement.bindLong(1, bookId);
        statement.bindLong(2, position.getParagraphIndex());
        statement.bindLong(3, position.getElementIndex());
        statement.bindLong(4, position.getCharIndex());

        long timestamp = -1;
        if (position instanceof ZLTextFixedPosition.WithTimestamp) {
            timestamp = ((ZLTextFixedPosition.WithTimestamp) position).Timestamp;
        }
        if (timestamp == -1) {
            timestamp = System.currentTimeMillis();
        }
        statement.bindLong(5, timestamp);

        statement.execute();
    }


    public void saveBookProgress(long bookId, RationalNumber progress) {
        final SQLiteStatement statement = get(
                "INSERT OR REPLACE INTO BookReadingProgress (book_id,numerator,denominator) VALUES (?,?,?)"
        );
        statement.bindLong(1, bookId);
        statement.bindLong(2, progress.Numerator);
        statement.bindLong(3, progress.Denominator);
        statement.execute();
    }

    protected RationalNumber getProgress(long bookId) {
        final RationalNumber progress;
        final Cursor cursor = myDatabase.rawQuery(
                "SELECT numerator,denominator FROM BookReadingProgress WHERE book_id=" + bookId, null
        );
        if (cursor.moveToNext()) {
            progress = RationalNumber.create(cursor.getLong(0), cursor.getLong(1));
        } else {
            progress = null;
        }
        cursor.close();
        return progress;
    }


    protected void deleteBook(long bookId) {
        myDatabase.beginTransaction();
        myDatabase.execSQL("DELETE FROM BookReadingProgress WHERE book_id=" + bookId);
        myDatabase.execSQL("DELETE FROM Books WHERE book_id=" + bookId);
        myDatabase.setTransactionSuccessful();
        myDatabase.endTransaction();
    }

    private void createTables() {
        myDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS Books(" +
                        "book_id INTEGER PRIMARY KEY," +
                        "encoding TEXT," +
                        "language TEXT," +
                        "title TEXT NOT NULL," +
                        "file_path TEXT UNIQUE NOT NULL)");

        myDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS BookReadingProgress(" +
                        "book_id INTEGER PRIMARY KEY REFERENCES Books(book_id)," +
                        "numerator INTEGER NOT NULL," +
                        "denominator INTEGER NOT NULL)");
        myDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS BookState(" +
                        "book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id)," +
                        "paragraph INTEGER NOT NULL," +
                        "word INTEGER NOT NULL," +
                        "char INTEGER NOT NULL," +
                        "timestamp INTEGER)");
    }


    private SQLiteStatement get(String sql) {
        SQLiteStatement statement = myStatements.get(sql);
        if (statement == null) {
            statement = myDatabase.compileStatement(sql);
            myStatements.put(sql, statement);
        }
        return statement;
    }
}

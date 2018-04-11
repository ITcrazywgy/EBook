package org.geometerplus.android.fbreader;

import org.geometerplus.fbreader.book.Book;

/**
 * Created by Felix on 2018/3/23 0023.
 */

public class BookLibrary {

    private static class SingletonHolder {
        private static BookLibrary instance = new BookLibrary();
    }

    public static BookLibrary getInstance() {
        return BookLibrary.SingletonHolder.instance;
    }

    private BookLibrary() {}

}

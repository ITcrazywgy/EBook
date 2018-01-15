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

package org.geometerplus.fbreader.book;

import org.geometerplus.fbreader.formats.BookReadingException;
import org.geometerplus.fbreader.formats.FormatPlugin;
import org.geometerplus.fbreader.formats.PluginCollection;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.util.SystemInfo;

public final class Book extends AbstractBook {
    private static final long serialVersionUID = -5732802077850327561L;
    private final String myPath;

    public Book(long id, String path, String title, String encoding, String language) {
        super(id, title, encoding, language);
        if (path == null) {
            throw new IllegalArgumentException("Creating book with no file");
        }
        myPath = path;
    }

    public Book(String path, SystemInfo systemInfo) throws BookReadingException {
        super(-1, null, null, null);
        ZLFile file = ZLFile.createFileByPath(path);
        PluginCollection pluginCollection = PluginCollection.Instance(systemInfo);
        FormatPlugin plugin = pluginCollection.getPlugin(file);
        file = plugin.realBookFile(file);
        if (file == null) {
            throw new IllegalArgumentException("Creating book with no file");
        }
        myPath = file.getPath();
        BookUtil.readMetainfo(this, plugin);
    }

    @Override
    public String getPath() {
        return myPath;
    }

    @Override
    public int hashCode() {
        return myPath.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book)) {
            return false;
        }
        return myPath.equals(((Book) o).myPath);
    }
}

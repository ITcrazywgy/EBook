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

package org.geometerplus.android.fbreader.config;

import android.content.Context;

import org.geometerplus.zlibrary.core.options.Config;

public final class ConfigShadow extends Config {
    private final Context myContext;


    public ConfigShadow(Context context) {
        myContext = context;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void runOnConnect(Runnable runnable) {

        runnable.run();
    }


    public boolean getSpecialBooleanValue(String name, boolean defaultValue) {
        return myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
                .getBoolean(name, defaultValue);
    }

    public void setSpecialBooleanValue(String name, boolean value) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
                .putBoolean(name, value).commit();
    }

    public String getSpecialStringValue(String name, String defaultValue) {
        return myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE)
                .getString(name, defaultValue);
    }

    public void setSpecialStringValue(String name, String value) {
        myContext.getSharedPreferences("fbreader.ui", Context.MODE_PRIVATE).edit()
                .putString(name, value).commit();
    }

    @Override
    protected String getValueInternal(String group, String name) throws NotAvailableException {
        return null;
    }

    @Override
    protected void setValueInternal(String group, String name, String value) {

    }

    @Override
    protected void unsetValueInternal(String group, String name) {

    }



}

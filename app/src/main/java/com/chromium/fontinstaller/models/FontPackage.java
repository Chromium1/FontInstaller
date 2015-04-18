/*
 * Copyright 2015 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chromium.fontinstaller.models;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by priyeshpatel on 15-02-06.
 */
public class FontPackage implements Parcelable {
    private String name;
    private String nameFormatted;
    private HashMap<Font, Style> fontList = new HashMap<>();

    private static final String BASE_URL = "https://raw.githubusercontent.com/ItsPriyesh/FontsterFontsRepo/master/";

    public FontPackage(String name) {
        this.name = name;
        this.nameFormatted = name.replace(" ", "") + "FontPack";

        generateFonts();
    }

    private void generateFonts() {
        for (Style style : Style.values()) {
            Font font = new Font(style, BASE_URL + nameFormatted + "/" + style.getRemoteName());
            fontList.put(font, style);
        }
    }

    public ArrayList<Font> getFontList() {
        return new ArrayList<>(fontList.keySet());
    }

    public String getName() {
        return name;
    }

    public String getNameFormatted() {
        return nameFormatted;
    }

    public Typeface getTypeface(Style style, Context context) {
        String path = context.getExternalCacheDir() + File.separator +
                nameFormatted + File.separator + style.getLocalName();

        if (new File(path).exists()) {
            try {
                return Typeface.createFromFile(path);
            } catch (Exception e) {
                return Typeface.DEFAULT;
            }
        } else return Typeface.DEFAULT;
    }

    public Font getFont(Style style) {
        for (Font font : fontList.keySet())
            if (fontList.get(font).equals(style))
                return font;

        return null;
    }

    protected FontPackage(Parcel in) {
        name = in.readString();
        nameFormatted = in.readString();

        Bundle bundle = in.readBundle();
        bundle.setClassLoader(ClassLoader.getSystemClassLoader());
        fontList = (HashMap<Font, Style>) bundle.getSerializable("fontList");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(nameFormatted);
        Bundle bundle = new Bundle();
        bundle.putSerializable("fontList", fontList);
        dest.writeBundle(bundle);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<FontPackage> CREATOR = new Parcelable.Creator<FontPackage>() {
        @Override
        public FontPackage createFromParcel(Parcel in) {
            return new FontPackage(in);
        }

        @Override
        public FontPackage[] newArray(int size) {
            return new FontPackage[size];
        }
    };
}
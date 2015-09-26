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

package com.chromium.fontinstaller.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.chromium.fontinstaller.models.Font;
import com.chromium.fontinstaller.models.FontPackage;
import com.chromium.fontinstaller.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import rx.Observable;

public class FontInstaller {

    private static final String MOUNT_SYSTEM = "mount -o rw,remount /system";
    private static final String FONT_INSTALL_DIR = "/system/fonts";

    @SuppressLint("SdCardPath")
    private static final String CACHE_DIR = "/sdcard/Android/data/com.chromium.fontinstaller/cache/";

    public static class InstallException extends Exception {
        public InstallException(Exception root) { super(root); }
    }

    public static Observable<Void> install(final FontPackage fontPackage, final Activity context) {
        List<String> copyCommands = new ArrayList<>();
        return Observable.create(subscriber -> {
            for (Font font : fontPackage.getFontList()) {
                final String file = CACHE_DIR + fontPackage.getNameFormatted() + File.separator + font.getName();
                if (!new File(file).exists()) {
                    subscriber.onError(new InstallException(new IOException("File not found!")));
                    return;
                }
                final String installCommand = "cp " + file + " " + FONT_INSTALL_DIR;
                Log.d("FontInstaller", "Adding command: " + installCommand);
                copyCommands.add(installCommand);
            }
            copyCommands.add(generateLockscreenFixCommand(context));
            if (Shell.SU.available()) {
                Shell.SU.run(MOUNT_SYSTEM);
                Shell.SU.run(copyCommands);
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        });
    }

    // This font file is copied as a workaround/fix to the lockscreen colon bug
    private static String generateLockscreenFixCommand(Context context) {
        return "cp " + FileUtils.getAssetsFile("DroidSansFallback.ttf", context)
                .getAbsolutePath() + " " + FONT_INSTALL_DIR;
    }

}

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

import static com.chromium.fontinstaller.core.SystemConstants.*;

public class FontInstaller {

  private static final String TAG = FontInstaller.class.toString();

  public static class InstallException extends Exception {
    public InstallException(Exception root) { super(root); }
  }

  public static Observable<Void> install(final FontPackage fontPackage, final Activity context) {
    final List<String> copyCommands = new ArrayList<>();
    return Observable.create(subscriber -> {
      for (Font font : fontPackage.getFontSet()) {
        final File file = font.getFile();
        if (!file.exists()) {
          subscriber.onError(new InstallException(new IOException("File not found!")));
          return;
        }
        final String installCommand = "cp " + file + " " + SYSTEM_FONT_PATH;
        Log.d(TAG, "Adding command: " + installCommand);
        copyCommands.add(installCommand);
      }

      String lockscreenFixCommand = generateLockscreenFixCommand(context);
      if (lockscreenFixCommand != null) copyCommands.add(lockscreenFixCommand);

      if (Shell.SU.available()) {
        Shell.SU.run(MOUNT_SYSTEM_COMMAND);
        Shell.SU.run(copyCommands);
        subscriber.onNext(null);
        subscriber.onCompleted();
      }
    });
  }

  // This font file is copied as a workaround/fix to the lockscreen colon bug
  private static String generateLockscreenFixCommand(Context context) {
    File fallbackFont = FileUtils.getAssetsFile("DroidSansFallback.ttf", context);
    return fallbackFont != null
        ? "cp " + fallbackFont.getAbsolutePath() + " " + SYSTEM_FONT_PATH
        : null;
  }

}

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

import android.content.Context;

import com.chromium.fontinstaller.models.Font;
import com.chromium.fontinstaller.models.FontPackage;
import com.chromium.fontinstaller.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import rx.Observable;

import static com.chromium.fontinstaller.core.SystemConstants.MOUNT_SYSTEM_COMMAND;
import static com.chromium.fontinstaller.core.SystemConstants.SYSTEM_FONT_PATH;

public class FontInstaller {

  private static final String INSTALL_FORMATTER = "cp %s " + SYSTEM_FONT_PATH;
  private static final String NO_OP_COMMAND = ":";

  public static class InstallException extends RuntimeException {
    public InstallException(IOException root) { super(root); }
    public InstallException(String message) { super(message); }
  }

  public static Observable<List<String>> install(FontPackage fontPackage, Context context) {
    return generateCommands(fontPackage, context)
        .collect(ArrayList<String>::new, ArrayList::add)
        .map(FontInstaller::runCommands);
  }

  /* package */ static Observable<String> generateCommands(FontPackage fontPackage, Context context) {
    return Observable.from(fontPackage.getFontSet())
        .map(FontInstaller::getFileOrThrow)
        .map(FontInstaller::createInstallCommand)
        .toList()
        .startWith(Arrays.asList(MOUNT_SYSTEM_COMMAND, createLockscreenFixCommand(context)))
        .flatMapIterable(commands -> commands);
  }

  private static List<String> runCommands(ArrayList<String> commands) throws InstallException {
    final List<String> result = Shell.SU.run(commands);
    if (result == null) throw new InstallException("Failed to run commands " + commands);
    return result;
  }

  private static File getFileOrThrow(Font font) throws InstallException {
    final File file = font.getFile();
    if (file != null && file.exists()) return file;
    else throw new InstallException(new IOException("File not found!"));
  }

  private static String createInstallCommand(File file) {
    return String.format(INSTALL_FORMATTER, file.getAbsolutePath());
  }

  // This font file is copied as a workaround/fix to the lockscreen colon bug
  private static String createLockscreenFixCommand(Context context) {
    File fallbackFont = FileUtils.getAssetsFile("DroidSansFallback.ttf", context);
    return fallbackFont != null
        ? String.format(INSTALL_FORMATTER, fallbackFont.getAbsolutePath())
        : NO_OP_COMMAND;
  }

}

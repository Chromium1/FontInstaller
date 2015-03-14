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

package com.chromium.fontinstaller.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by priyeshpatel on 15-02-16.
 */
public class RootUtils {
    private static ProgressDialog progressDialog;

    public static void runBackgroundCommand(String command, boolean showProgress, Context context) {
        class Task extends AsyncTask<String, Void, Void> {
            @Override
            protected void onPreExecute() {
                if (showProgress) {
                    progressDialog = new ProgressDialog(context);
                    progressDialog.setMessage("Loading");
                }
            }

            @Override
            protected Void doInBackground(String... commands) {
                Shell.SU.run(commands);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (showProgress) {
                    progressDialog.dismiss();
                }
            }
        }
        new Task().execute(command);
    }

    public static void requestAccess() {
        new Thread(() -> {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Shell.SU.available();
        }).start();
    }

}

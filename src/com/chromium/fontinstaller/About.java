/*
 * Copyright (C) 2014 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chromium.fontinstaller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.chromium.fontinstaller.util.IabHelper;
import com.chromium.fontinstaller.util.IabResult;
import com.chromium.fontinstaller.util.Inventory;
import com.chromium.fontinstaller.util.Purchase;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class About extends PreferenceActivity {

	int easterEggClicks = 10, counter;
	private static final String TAG = "com.chromium.fontinstaller";
	IabHelper mHelper;
	static final String ITEM_SKU = "com.chromium.fontster.donate";
	ProgressDialog downloadProgress, mProgressDialog;

	String StorezipFileLocation = Environment.getExternalStorageDirectory() + "/Fontster/ListPreviews/ListPreviews.zip"; 
	String DirectoryName = Environment.getExternalStorageDirectory() + "/Fontster/ListPreviews/";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preference);	

		ActionBar ab = getActionBar();
		ab.setDisplayHomeAsUpEnabled(true);

		String base64EncodedPublicKey = 
				"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjZla6YOR6" +
						"Od3NTxs98KfxlQV68oTdGZmiRO2vzVIQ7W3oYP6FjNpjMhyckjvkD" +
						"2sCtHBPU6OjkSD75TzB17knW2FK7ZZGQkEMdCLD9qX8IbJ17w0UDG" +
						"pW3b71sOWiOV92f0aIzgRZFl7IERzsgzFEnpCLx3Yxdl4JLmErPaE" +
						"19ZxHeJ+r25O4NMYCmzAPHdmtBOKRiNGqg6gQPsZqBlfy0XV+pEZB" +
						"7HagpMlaYvy0eBXA5PkvtpRjDciukOw3j6hqTK/FHepNC4PxO1BWB" +
						"jGaeZh4u/HbGzXplI8UHZOThfUpHWqzIM3kT8bJ9+JfrViM7pLYpB" +
						"Sy9+xhIGIcKHKkQIDAQAB";

		mHelper = new IabHelper(this, base64EncodedPublicKey);

		/**
		 * Method to start in-app billing service for donations
		 */
		boolean inAppBillingEnabled = false; //set false for emulator compatibility
		if (inAppBillingEnabled){
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) {
					if (!result.isSuccess()) {
						Log.d(TAG, "In-app Billing setup failed: " + 
								result);
					} else {             
						Log.d(TAG, "In-app Billing is set up OK");
					}
					mHelper.queryInventoryAsync(mReceivedInventoryListener);
				}
			});
		}

		/**
		 * Listens for click of the 'Enable True Font Display' preference
		 * When clicked, this will download the zip containing the previews
		 * then extract the file onto the users storage. Later when the user
		 * opens the FontList activity the font titles will be displayed in
		 * their respective fonts.
		 */
		Preference displayFontsInList = (Preference) findPreference("displayFontsInList");
		displayFontsInList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {

				File dir = new File(Environment.getExternalStorageDirectory() + "/Fontster/ListPreviews/");

				if(dir.exists())  {
					CustomAlerts.showBasicAlert("Already enabled", "This option is already being used.", About.this);
				}
				else {
					if (haveNetworkConnection()){ //connection available								

						DownloadManager.Request downloadPreviewZip = new DownloadManager.Request(Uri.parse("https://github.com/Chromium1/Fonts/raw/master/ListPreviews.zip"));
						downloadPreviewZip.allowScanningByMediaScanner();
						downloadPreviewZip.setDestinationInExternalPublicDir("/Fontster/ListPreviews/", "ListPreviews.zip");
						downloadPreviewZip.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);	

						DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

						//display a progress dialog just before the request is sent
						downloadProgress = new ProgressDialog(About.this);
						downloadProgress.setMessage("Downloading font previews...");
						downloadProgress.show();

						manager.enqueue(downloadPreviewZip);
						counter = 1;

						// listen for download completion, and close the progress dialog once it is detected
						BroadcastReceiver receiver1 = new BroadcastReceiver() {
							@Override
							public void onReceive(Context context, Intent intent) {
								String action1 = intent.getAction();
								if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action1)) {
									counter--; //reduce value to 0, indicating download completion					
								}
								if (counter == 0){
									downloadProgress.dismiss();
									// Extract zip
									try {
										unzip();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						};
						registerReceiver(receiver1, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
					}
					else { //no connection
						CustomAlerts.showBasicAlert("No connection", "Your phone must be connected to the internet.", About.this);
					}
				}
				return true; 
			}
		});

		/**
		 * Listens for click of the 'Disable True Font Display' preference
		 * When clicked, this will delete the ListPreviews directory 
		 * created by the option above, hence disabling the font names
		 * being shown in their actual fonts.
		 */
		Preference disableDisplayFontsInList = (Preference) findPreference("disableDisplayFontsInList");
		disableDisplayFontsInList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				File previewListDir = new File("/sdcard/Fontster/ListPreviews");

				if (previewListDir.exists() || previewListDir.exists()) {
					AsyncTask<Void, Void, Void> revertFontsInListView = new AsyncTask<Void, Void, Void>()  { 
						//display progress dialog while fonts are copied in background
						ProgressDialog deletionProgress;

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
							deletionProgress = new ProgressDialog (About.this);
							deletionProgress.setMessage("Reverting 'Enable True Font Display' option...");
							deletionProgress.show();
						}

						@Override
						protected Void doInBackground(Void... params) {

							String previews = "rm -r /sdcard/Fontster/ListPreviews";
							Runtime runtime = Runtime.getRuntime();
							try {
								runtime.exec(previews);
							}
							catch (IOException e) { 

							}
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							if (deletionProgress != null) {
								if (deletionProgress.isShowing()) {
									deletionProgress.dismiss();
								}
							}
							CustomAlerts.showBasicAlert ("Done", "'True Font Display' has been disabled. Restart the app for changes to take effect.", About.this);
						}
					};
					revertFontsInListView.execute((Void[])null);

				}
				else
					CustomAlerts.showBasicAlert ("Not necessary", "'True Font Display' has not been enabled. No need to undo it.", About.this);
				return true; 
			}
		});

		/**
		 * Listens for click of the 'Clear cache' preference
		 * When clicked, this will delete the DownloadedFonts
		 * and SampleFonts directories.
		 */
		Preference clearCache = (Preference) findPreference("clearCache");
		clearCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {

				File downloadDir = new File("/sdcard/Fontster/DownloadedFonts");
				File previewDir = new File("/sdcard/Fontster/SampleFonts");

				if (downloadDir.exists() || previewDir.exists()) {
					AsyncTask<Void, Void, Void> cleanCache = new AsyncTask<Void, Void, Void>()  { 
						//display progress dialog while fonts are copied in background
						ProgressDialog cleanCacheProgress;

						@Override
						protected void onPreExecute() {
							super.onPreExecute();
							cleanCacheProgress = new ProgressDialog (About.this);
							cleanCacheProgress.setMessage("Clearing downloaded fonts...");
							cleanCacheProgress.show();
						}

						@Override
						protected Void doInBackground(Void... params) {

							String downloads = "rm -r /sdcard/Fontster/DownloadedFonts";
							String previews = "rm -r /sdcard/Fontster/SampleFonts";
							Runtime runtime = Runtime.getRuntime();
							try {
								runtime.exec(downloads);
								runtime.exec(previews);
							}
							catch (IOException e) { 

							}
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							super.onPostExecute(result);
							if (cleanCacheProgress != null) {
								if (cleanCacheProgress.isShowing()) {
									cleanCacheProgress.dismiss();
								}
							}
							CustomAlerts.showBasicAlert ("Done", "Cached fonts have been deleted.", About.this);
						}
					};
					cleanCache.execute((Void[])null);
				}
				else
					CustomAlerts.showBasicAlert ("Nothing to clean", "There are currently no cached fonts.", About.this);
				return true; 
			}
		});

		/**
		 * Takes value inputted by user into textfield and 
		 * sends email containing this string.
		 */
		Preference requestFont = (Preference) findPreference("fontRequest");
		requestFont.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				CustomAlerts.showRequestAlert(About.this);	
				return true; 
			}
		});

		/**
		 * First shows a confirmation, then reboots the
		 * device when the user clicks yes to the promt.
		 */
		Preference reboot = (Preference) findPreference("reboot");
		reboot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {

				final Dialog confirm = new Dialog(About.this);

				confirm.requestWindowFeature(Window.FEATURE_NO_TITLE);
				confirm.setContentView(R.layout.two_button_alert);	
				TextView alertTitle = (TextView) confirm.findViewById(R.id.title);
				alertTitle.setText("Confirm reboot");
				TextView alertMessage = (TextView) confirm.findViewById(R.id.message);
				alertMessage.setText("Are you sure you want to reboot?");
				Button positiveButton = (Button) confirm.findViewById(R.id.positive);
				Button negativeButton = (Button) confirm.findViewById(R.id.negative);

				positiveButton.setOnClickListener(new View.OnClickListener() { // confirm yes
					public void onClick(View v){
						confirm.cancel();
						try {
							Process softReboot = Runtime.getRuntime().exec(new String[] { "su", "-c", "reboot"});	
						} 
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				});	
				negativeButton.setOnClickListener(new View.OnClickListener() { // confirm no
					public void onClick(View v){
						confirm.cancel();
					}			
				});
				confirm.show();
				return true; 
			}
		});

		/**
		 * Refreshes the SystemUI of the system
		 * by excecuting a shell command.
		 */
		Preference restartSysUI = (Preference) findPreference("restartSysUI");
		restartSysUI.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				try {
					Process restartSysUI = Runtime.getRuntime().exec(new String[] { "su", "-c", "pkill com.android.systemui"});	
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				return true; 
			}
		});

		/**
		 * Opens the Splash activity.
		 */
		Preference splash = (Preference) findPreference("viewSplash");
		splash.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent openSplash = new Intent (About.this, Splash.class);
				startActivity(openSplash);				
				return true; 
			}
		});

		/**
		 * Launches the In App Billing Service.
		 */
		Preference donateInAppBilling = (Preference) findPreference("inAppBilling");
		donateInAppBilling.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {				
				mHelper.launchPurchaseFlow(About.this, ITEM_SKU, 10001, mPurchaseFinishedListener, "mypurchasetoken");
				return true; 
			}
		});

		/**
		 * Opens the GitHub repository of the app.
		 */
		Preference source = (Preference) findPreference("source");
		source.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent git = new Intent();
				git.setAction(Intent.ACTION_VIEW);
				git.addCategory(Intent.CATEGORY_BROWSABLE);
				git.setData(Uri.parse("https://github.com/Chromium1/FontInstaller"));
				startActivity(git);
				return true; 
			}
		});

		/**
		 * Opens our website - fontster.cf
		 */
		Preference site = (Preference) findPreference("website");
		site.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent site = new Intent();
				site.setAction(Intent.ACTION_VIEW);
				site.addCategory(Intent.CATEGORY_BROWSABLE);
				site.setData(Uri.parse("https://fontster.cf"));
				startActivity(site);
				return true; 
			}
		});

		/**
		 * Sends the developer an email.
		 */
		Preference contact = (Preference) findPreference("contact");
		contact.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent email = new Intent();
				email.setAction(Intent.ACTION_VIEW);
				email.addCategory(Intent.CATEGORY_BROWSABLE);
				email.setData(Uri.parse("mailto:priyesh.96@hotmail.com"));
				startActivity(email);
				return true; 
			}
		});

		/**
		 * Opens our PayPal donation link
		 */
		Preference donate = (Preference) findPreference("donate");
		donate.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Intent donate = new Intent();
				donate.setAction(Intent.ACTION_VIEW);
				donate.addCategory(Intent.CATEGORY_BROWSABLE);
				donate.setData(Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations" +
						"&business=XR9WBFXGZ9G5E&lc=CA&item_name=Font%20Installer&currency_code" +
						"=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted"));
				startActivity(donate);
				return true; 
			}
		});

		Preference ethan = (Preference) findPreference("ethan");
		ethan.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				easterEggClicks--;
				if (easterEggClicks < 0){
					Intent aboutEE = new Intent(About.this, AboutEE.class);
					startActivity(aboutEE);
					easterEggClicks = 10;
				}
				return true; 
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {     
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			if (result.isFailure()) {
				Toast.makeText(getApplicationContext(), "Failed to make purchase.", Toast.LENGTH_LONG).show();
				return;
			}      
			else if (purchase.getSku().equals(ITEM_SKU)) {
				consumeItem();
			}
		}
	};

	public void consumeItem() {
		mHelper.queryInventoryAsync(mReceivedInventoryListener);
	}

	IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			if (result.isFailure()) {
				// Handle failure				
			}
			Purchase donate = inventory.getPurchase(ITEM_SKU);

			if (donate != null) {
				mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU), mConsumeFinishedListener);
			}
		}
	};

	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			if (result.isSuccess()) {		    	 
				CustomAlerts.showBasicAlert("Thanks", "We appreciate your support.", About.this);
			} else {
				// handle error
			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mHelper != null) mHelper.dispose();
		mHelper = null;
	}

	public void unzip() throws IOException {
		mProgressDialog = new ProgressDialog(About.this);
		mProgressDialog.setMessage("Extracting previews...");
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCancelable(false);
		mProgressDialog.show();
		new UnZipTask().execute(StorezipFileLocation, DirectoryName);
	}

	private class UnZipTask extends AsyncTask<String, Void, Boolean> {
		@SuppressWarnings("rawtypes")
		@Override
		protected Boolean doInBackground(String... params) {
			String filePath = params[0];
			String destinationPath = params[1];

			File archive = new File(filePath);
			try {
				ZipFile zipfile = new ZipFile(archive);
				for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
					ZipEntry entry = (ZipEntry) e.nextElement();
					unzipEntry(zipfile, entry, destinationPath);
				}

				UnzipUtil d = new UnzipUtil(StorezipFileLocation, DirectoryName); 
				d.unzip();
			} 
			catch (Exception e) {
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressDialog.dismiss();
			CustomAlerts.showBasicAlert("Previews downloaded", "Font previews for the list have been successfully saved onto your device", About.this);
		}

		private void unzipEntry(ZipFile zipfile, ZipEntry entry,String outputDir) throws IOException {

			if (entry.isDirectory()) {
				createDir(new File(outputDir, entry.getName()));
				return;
			}

			File outputFile = new File(outputDir, entry.getName());
			if (!outputFile.getParentFile().exists()) {
				createDir(outputFile.getParentFile());
			}

			// Log.v("", "Extracting: " + entry);
			BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

			try {
			}
			finally {
				outputStream.flush();
				outputStream.close();
				inputStream.close();
			}
		}

		private void createDir(File dir) {
			if (dir.exists()) {
				return;
			}
			if (!dir.mkdirs()) {
				throw new RuntimeException("Can not create dir " + dir);
			}
		}

	}

	/**
	 * Checks if users device has an internet connection,
	 * to either WiFi or Mobile Data.
	 * @return Returns true if connection is available, and false if it is not
	 */
	private boolean haveNetworkConnection() {
		boolean haveConnectedWifi = false;
		boolean haveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					haveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					haveConnectedMobile = true;
		}
		return haveConnectedWifi || haveConnectedMobile;
	}
}

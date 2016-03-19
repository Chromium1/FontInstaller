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

package com.chromium.fontinstaller.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;

import com.chromium.fontinstaller.BuildConfig;
import com.chromium.fontinstaller.R;
import com.chromium.fontinstaller.ui.backuprestore.BackupRestoreFragment;
import com.chromium.fontinstaller.ui.common.BaseActivity;
import com.chromium.fontinstaller.ui.fontlist.FontListFragment;
import com.chromium.fontinstaller.ui.settings.SettingsActivity;
import com.google.android.gms.ads.AdView;

import butterknife.Bind;

public class MainActivity extends BaseActivity {

  @Bind(R.id.drawer_layout)
  DrawerLayout mDrawerLayout;

  @Bind(R.id.navigation_view)
  NavigationView mNavigationView;

  @Bind(R.id.ad_view)
  AdView mAdView;

  private ActionBarDrawerToggle mDrawerToggle;
  private FragmentManager mFragmentManager;
  private FontListFragment mFontListFragment;
  private BackupRestoreFragment mBackupRestoreFragment;

  private static final String EXTRA_PAGE_ID = "extra_page_id";

  public static final int FONT_LIST = 0xcafe;
  public static final int BACKUP_RESTORE = 0xbac;

  public static Intent getLaunchIntent(int pageId, Context context) {
    final Intent intent = new Intent(context, MainActivity.class);
    intent.putExtra(EXTRA_PAGE_ID, pageId);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setToolbarTitle(getString(R.string.app_name));

    if (!BuildConfig.DEBUG) initializeAd(mAdView);

    mDrawerToggle = new ActionBarDrawerToggle(
        this, mDrawerLayout, mToolbar, R.string.drawer_open, R.string.drawer_close);

    setupDrawerContent(mNavigationView);
    mDrawerLayout.setDrawerListener(mDrawerToggle);

    mFragmentManager = getSupportFragmentManager();
    mFontListFragment = new FontListFragment();
    mBackupRestoreFragment = new BackupRestoreFragment();

    swapFragment(mFontListFragment);
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  @Override protected void onPostResume() {
    super.onPostResume();
    int pageId = getIntent().getIntExtra(EXTRA_PAGE_ID, -1);
    if (pageId != -1) swapFragment(fragmentFromPageId(pageId));
  }

  private Fragment fragmentFromPageId(int pageId) {
    switch (pageId) {
      case FONT_LIST: return mFontListFragment;
      case BACKUP_RESTORE: return mBackupRestoreFragment;
      default: return mFontListFragment;
    }
  }

  private void setupDrawerContent(NavigationView navigationView) {
    navigationView.setNavigationItemSelectedListener(menuItem -> {
      mDrawerLayout.closeDrawers();
      selectDrawerItem(menuItem);
      return true;
    });
  }

  private void selectDrawerItem(MenuItem menuItem) {
    final int selectedId = menuItem.getItemId();
    switch (selectedId) {
      case R.id.fonts:
        swapFragment(mFontListFragment);
        menuItem.setChecked(true);
        setTitle(getString(R.string.app_name));
        invalidateOptionsMenu();
        break;
      case R.id.backup:
        swapFragment(mBackupRestoreFragment);
        menuItem.setChecked(true);
        setTitle(getString(R.string.drawer_item_backup_restore));
        invalidateOptionsMenu();
        break;
      case R.id.settings:
        final Intent intent = new Intent(this, SettingsActivity.class);
        delay(() -> startActivity(intent), 200);
        break;
    }
  }

  private void swapFragment(Fragment fragment) {
    mFragmentManager.beginTransaction().replace(R.id.container, fragment).commit();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    mDrawerToggle.syncState();
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override public void onBackPressed() {
    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
      mDrawerLayout.closeDrawers();
      return;
    }
    super.onBackPressed();
  }
}

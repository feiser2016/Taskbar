/* Copyright 2020 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0(the "License");
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

package com.farmerbb.taskbar.backup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.Process;
import android.os.UserManager;

import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.BlacklistEntry;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.SavedWindowSizesEntry;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class BackupUtils {

    private BackupUtils() {}

    public static void backup(Context context, BackupAgent agent) {
        // Get pinned and blocked apps
        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        List<AppEntry> pinnedAppsList = pba.getPinnedApps();

        String[] pinnedAppsPackageNames = new String[pinnedAppsList.size()];
        String[] pinnedAppsComponentNames = new String[pinnedAppsList.size()];
        String[] pinnedAppsLabels = new String[pinnedAppsList.size()];
        long[] pinnedAppsUserIds = new long[pinnedAppsList.size()];

        for(int i = 0; i < pinnedAppsList.size(); i++) {
            AppEntry entry = pinnedAppsList.get(i);
            pinnedAppsPackageNames[i] = entry.getPackageName();
            pinnedAppsComponentNames[i] = entry.getComponentName();
            pinnedAppsLabels[i] = entry.getLabel();
            pinnedAppsUserIds[i] = entry.getUserId(context);
        }

        agent.putStringArray("pinned_apps_package_names", pinnedAppsPackageNames);
        agent.putStringArray("pinned_apps_component_names", pinnedAppsComponentNames);
        agent.putStringArray("pinned_apps_labels", pinnedAppsLabels);
        agent.putLongArray("pinned_apps_user_ids", pinnedAppsUserIds);

        List<AppEntry> blockedAppsList = pba.getBlockedApps();

        String[] blockedAppsPackageNames = new String[blockedAppsList.size()];
        String[] blockedAppsComponentNames = new String[blockedAppsList.size()];
        String[] blockedAppsLabels = new String[blockedAppsList.size()];

        for(int i = 0; i < blockedAppsList.size(); i++) {
            AppEntry entry = blockedAppsList.get(i);
            blockedAppsPackageNames[i] = entry.getPackageName();
            blockedAppsComponentNames[i] = entry.getComponentName();
            blockedAppsLabels[i] = entry.getLabel();
        }

        agent.putStringArray("blocked_apps_package_names", blockedAppsPackageNames);
        agent.putStringArray("blocked_apps_component_names", blockedAppsComponentNames);
        agent.putStringArray("blocked_apps_labels", blockedAppsLabels);

        // Get blacklist
        Blacklist blacklist = Blacklist.getInstance(context);
        List<BlacklistEntry> blacklistList = blacklist.getBlockedApps();

        String[] blacklistPackageNames = new String[blacklistList.size()];
        String[] blacklistLabels = new String[blacklistList.size()];

        for(int i = 0; i < blacklistList.size(); i++) {
            BlacklistEntry entry = blacklistList.get(i);
            blacklistPackageNames[i] = entry.getPackageName();
            blacklistLabels[i] = entry.getLabel();
        }

        agent.putStringArray("blacklist_package_names", blacklistPackageNames);
        agent.putStringArray("blacklist_labels", blacklistLabels);

        // Get top apps
        TopApps topApps = TopApps.getInstance(context);
        List<BlacklistEntry> topAppsList = topApps.getTopApps();

        String[] topAppsPackageNames = new String[topAppsList.size()];
        String[] topAppsLabels = new String[topAppsList.size()];

        for(int i = 0; i < topAppsList.size(); i++) {
            BlacklistEntry entry = topAppsList.get(i);
            topAppsPackageNames[i] = entry.getPackageName();
            topAppsLabels[i] = entry.getLabel();
        }

        agent.putStringArray("top_apps_package_names", topAppsPackageNames);
        agent.putStringArray("top_apps_labels", topAppsLabels);

        // Get saved window sizes
        if(U.canEnableFreeform()) {
            SavedWindowSizes savedWindowSizes = SavedWindowSizes.getInstance(context);
            List<SavedWindowSizesEntry> savedWindowSizesList = savedWindowSizes.getSavedWindowSizes();

            String[] savedWindowSizesComponentNames = new String[savedWindowSizesList.size()];
            String[] savedWindowSizesWindowSizes = new String[savedWindowSizesList.size()];

            for(int i = 0; i < savedWindowSizesList.size(); i++) {
                SavedWindowSizesEntry entry = savedWindowSizesList.get(i);
                savedWindowSizesComponentNames[i] = entry.getComponentName();
                savedWindowSizesWindowSizes[i] = entry.getWindowSize();
            }

            agent.putStringArray("saved_window_sizes_component_names", savedWindowSizesComponentNames);
            agent.putStringArray("saved_window_sizes_window_sizes", savedWindowSizesWindowSizes);
        }

        // Get shared preferences
        StringBuilder preferences = new StringBuilder();

        try {
            File file = new File(context.getFilesDir().getParent() + "/shared_prefs/" + context.getPackageName() + "_preferences.xml");
            FileInputStream input = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(input);
            BufferedReader buffer = new BufferedReader(reader);

            String line = buffer.readLine();
            while(line != null) {
                preferences.append(line);
                line = buffer.readLine();
                if(line != null)
                    preferences.append("\n");
            }

            reader.close();
        } catch (IOException e) { /* Gracefully fail */ }

        agent.putString("preferences", preferences.toString());
    }

    public static void restore(Context context, BackupAgent agent) {
        // Get pinned and blocked apps
        PinnedBlockedApps pba = PinnedBlockedApps.getInstance(context);
        pba.clear(context);

        String[] pinnedAppsPackageNames = agent.getStringArray("pinned_apps_package_names");
        String[] pinnedAppsComponentNames = agent.getStringArray("pinned_apps_component_names");
        String[] pinnedAppsLabels = agent.getStringArray("pinned_apps_labels");
        long[] pinnedAppsUserIds = agent.getLongArray("pinned_apps_user_ids");

        UserManager userManager =(UserManager) context.getSystemService(Context.USER_SERVICE);
        LauncherApps launcherApps =(LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        if(pinnedAppsPackageNames != null && pinnedAppsComponentNames != null && pinnedAppsLabels != null)
            for(int i = 0; i < pinnedAppsPackageNames.length; i++) {
                Intent throwaway = new Intent();
                throwaway.setComponent(ComponentName.unflattenFromString(pinnedAppsComponentNames[i]));

                long userId;
                if(pinnedAppsUserIds != null)
                    userId = pinnedAppsUserIds[i];
                else
                    userId = userManager.getSerialNumberForUser(Process.myUserHandle());

                AppEntry newEntry = new AppEntry(
                        pinnedAppsPackageNames[i],
                        pinnedAppsComponentNames[i],
                        pinnedAppsLabels[i],
                        IconCache.getInstance(context).getIcon(
                                context,
                                launcherApps.resolveActivity(throwaway, userManager.getUserForSerialNumber(userId))),
                        true
                );

                newEntry.setUserId(userId);
                pba.addPinnedApp(context, newEntry);
            }

        String[] blockedAppsPackageNames = agent.getStringArray("blocked_apps_package_names");
        String[] blockedAppsComponentNames = agent.getStringArray("blocked_apps_component_names");
        String[] blockedAppsLabels = agent.getStringArray("blocked_apps_labels");

        if(blockedAppsPackageNames != null && blockedAppsComponentNames != null && blockedAppsLabels != null)
            for(int i = 0; i < blockedAppsPackageNames.length; i++) {
                pba.addBlockedApp(context, new AppEntry(
                        blockedAppsPackageNames[i],
                        blockedAppsComponentNames[i],
                        blockedAppsLabels[i],
                        null,
                        false
                ));
            }

        // Get blacklist
        Blacklist blacklist = Blacklist.getInstance(context);
        blacklist.clear(context);

        String[] blacklistPackageNames = agent.getStringArray("blacklist_package_names");
        String[] blacklistLabels = agent.getStringArray("blacklist_labels");

        if(blacklistPackageNames != null && blacklistLabels != null)
            for(int i = 0; i < blacklistPackageNames.length; i++) {
                blacklist.addBlockedApp(context, new BlacklistEntry(
                        blacklistPackageNames[i],
                        blacklistLabels[i]
                ));
            }

        // Get top apps
        TopApps topApps = TopApps.getInstance(context);
        topApps.clear(context);

        String[] topAppsPackageNames = agent.getStringArray("top_apps_package_names");
        String[] topAppsLabels = agent.getStringArray("top_apps_labels");

        if(topAppsPackageNames != null && topAppsLabels != null)
            for(int i = 0; i < topAppsPackageNames.length; i++) {
                topApps.addTopApp(context, new BlacklistEntry(
                        topAppsPackageNames[i],
                        topAppsLabels[i]
                ));
            }

        // Get saved window sizes
        if(U.canEnableFreeform()) {
            SavedWindowSizes savedWindowSizes = SavedWindowSizes.getInstance(context);
            savedWindowSizes.clear(context);

            String[] savedWindowSizesComponentNames = agent.getStringArray("saved_window_sizes_component_names");
            String[] savedWindowSizesWindowSizes = agent.getStringArray("saved_window_sizes_window_sizes");

            if(savedWindowSizesComponentNames != null && savedWindowSizesWindowSizes != null)
                for(int i = 0; i < savedWindowSizesComponentNames.length; i++) {
                    savedWindowSizes.setWindowSize(context,
                            savedWindowSizesComponentNames[i],
                            savedWindowSizesWindowSizes[i]
                    );
                }
        }

        // Get shared preferences
        String contents = agent.getString("preferences");
        if(contents.length() > 0)
            try {
                File file = new File(context.getFilesDir().getParent() + "/shared_prefs/" + context.getPackageName() + "_preferences.xml");
                FileOutputStream output = new FileOutputStream(file);
                output.write(contents.getBytes());
                output.close();
            } catch (IOException e) { /* Gracefully fail */ }
    }
}
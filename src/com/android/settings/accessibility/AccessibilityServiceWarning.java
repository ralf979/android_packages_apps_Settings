/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.storage.StorageManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.List;

/**
 * Utility class for creating the dialog that asks users for explicit permission to grant
 * all of the requested capabilities to an accessibility service before the service is enabled
 */
public class AccessibilityServiceWarning {
    public static Dialog createCapabilitiesDialog(Activity parentActivity,
            AccessibilityServiceInfo info, DialogInterface.OnClickListener listener) {
        final AlertDialog ad = new AlertDialog.Builder(parentActivity)
                .setTitle(parentActivity.getString(R.string.enable_service_title,
                        info.getResolveInfo().loadLabel(parentActivity.getPackageManager())))
                .setView(createEnableDialogContentView(parentActivity, info))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, listener)
                .create();

        final View.OnTouchListener filterTouchListener = (View v, MotionEvent event) -> {
            // Filter obscured touches by consuming them.
            if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Toast.makeText(v.getContext(), R.string.touch_filtered_warning,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        };

        ad.create();
        ad.getButton(AlertDialog.BUTTON_POSITIVE).setOnTouchListener(filterTouchListener);
        return ad;
    }

    /**
     * Return whether the device is encrypted with legacy full disk encryption. Newer devices
     * should be using File Based Encryption.
     *
     * @return true if device is encrypted
     */
    private static boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    private static View createEnableDialogContentView(Activity parentActivity,
            AccessibilityServiceInfo info) {
        LayoutInflater inflater = (LayoutInflater) parentActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                null);

        TextView encryptionWarningView = (TextView) content.findViewById(
                R.id.encryption_warning);
        if (isFullDiskEncrypted()) {
            String text = parentActivity.getString(R.string.enable_service_encryption_warning,
                    info.getResolveInfo().loadLabel(parentActivity.getPackageManager()));
            encryptionWarningView.setText(text);
            encryptionWarningView.setVisibility(View.VISIBLE);
        } else {
            encryptionWarningView.setVisibility(View.GONE);
        }

        TextView capabilitiesHeaderView = (TextView) content.findViewById(
                R.id.capabilities_header);
        capabilitiesHeaderView.setText(parentActivity.getString(R.string.capabilities_list_title,
                info.getResolveInfo().loadLabel(parentActivity.getPackageManager())));

        LinearLayout capabilitiesView = (LinearLayout) content.findViewById(R.id.capabilities);

        // This capability is implicit for all services.
        View capabilityView = inflater.inflate(
                com.android.internal.R.layout.app_permission_item_old, null);

        ImageView imageView = (ImageView) capabilityView.findViewById(
                com.android.internal.R.id.perm_icon);
        imageView.setImageDrawable(parentActivity.getDrawable(
                com.android.internal.R.drawable.ic_text_dot));

        TextView labelView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_group);
        labelView.setText(parentActivity.getString(
                R.string.capability_title_receiveAccessibilityEvents));

        TextView descriptionView = (TextView) capabilityView.findViewById(
                com.android.internal.R.id.permission_list);
        descriptionView.setText(
                parentActivity.getString(R.string.capability_desc_receiveAccessibilityEvents));

        List<AccessibilityServiceInfo.CapabilityInfo> capabilities =
                info.getCapabilityInfos(parentActivity);

        capabilitiesView.addView(capabilityView);

        // Service-specific capabilities.
        final int capabilityCount = capabilities.size();
        for (int i = 0; i < capabilityCount; i++) {
            AccessibilityServiceInfo.CapabilityInfo capability = capabilities.get(i);

            capabilityView = inflater.inflate(
                    com.android.internal.R.layout.app_permission_item_old, null);

            imageView = (ImageView) capabilityView.findViewById(
                    com.android.internal.R.id.perm_icon);
            imageView.setImageDrawable(parentActivity.getDrawable(
                    com.android.internal.R.drawable.ic_text_dot));

            labelView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_group);
            labelView.setText(parentActivity.getString(capability.titleResId));

            descriptionView = (TextView) capabilityView.findViewById(
                    com.android.internal.R.id.permission_list);
            descriptionView.setText(parentActivity.getString(capability.descResId));

            capabilitiesView.addView(capabilityView);
        }

        return content;
    }
}

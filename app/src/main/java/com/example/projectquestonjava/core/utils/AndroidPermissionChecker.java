package com.example.projectquestonjava.core.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;



@Singleton
public class AndroidPermissionChecker implements PermissionChecker {
    private final Context context;

    @Inject
    public AndroidPermissionChecker(@ApplicationContext Context context) {
        this.context = context;
    }

    @Override
    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true; // Для версий ниже TIRAMISU разрешение не требуется явно
        }
    }
}
package dev.hyzor.patches.nova.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val NOVA_LAUNCHER = Compatibility(
        name = "Nova Launcher",
        packageName = "com.teslacoilsw.launcher",

        // The tested artifact is the APK from APKMirror (arm64-v8a + armeabi-v7a, nodpi).
        // It is a single APK, not a bundle, and all of the code the patch touches is in
        // classes.dex.
        apkFileType = ApkFileType.APK,
        appIconColor = 0xE53935,

        targets = listOf(
            // Nova's versionName is "89500 (8.9.5)", not "8.9.5"; Morphe compares the
            // versionName as-is, so the build number is part of the target string.
            AppTarget(version = "89500 (8.9.5)", versionCode = 89500),
        ),
    )
}

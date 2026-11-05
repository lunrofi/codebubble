package com.bmo.codebubble;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

/** Panoya kopyalama; istenirse Android 13+ hassas işaretini ekler. */
public final class ClipboardHelper {
    private ClipboardHelper() {}

    public static void copy(Context context, String code, boolean sensitive) {
        try {
            ClipboardManager cm = (ClipboardManager)
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return;
            ClipData clip = ClipData.newPlainText("Kod", code);
            if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PersistableBundle extras = new PersistableBundle();
                extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
                clip.getDescription().setExtras(extras);
            }
            cm.setPrimaryClip(clip);
        } catch (Exception ignored) {
        }
    }
}

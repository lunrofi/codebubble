package com.bmo.codespray;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

/** Diğer uygulamaların bildirimlerini dinler, kod içerenleri yakalar. */
public class CodeNotificationListener extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pkg = sbn.getPackageName();
        if (getPackageName().equals(pkg)) return;
        if (isIgnoredApp(pkg)) return;
        Notification notif = sbn.getNotification();
        if (notif == null) return;
        Bundle extras = notif.extras;
        if (extras == null) return;
        StringBuilder text = new StringBuilder();
        append(text, extras.getCharSequence(Notification.EXTRA_TITLE));
        append(text, extras.getCharSequence(Notification.EXTRA_TEXT));
        append(text, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null) {
            for (CharSequence line : lines) append(text, line);
        }
        String code = Detector.extract(this, text.toString());
        if (code != null) {
            CodeHandler.handle(this, code, pkg, text.toString(), "app", pkg);
        }
    }

    private boolean isIgnoredApp(String pkg) {
        String ignored = getSharedPreferences("codespray", MODE_PRIVATE)
                .getString("ignored_apps", "");
        for (String line : ignored.split("\n")) {
            if (!line.trim().isEmpty() && pkg.equals(line.trim())) {
                return true;
            }
        }
        return false;
    }

    private static void append(StringBuilder sb, CharSequence cs) {
        if (!TextUtils.isEmpty(cs)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(cs);
        }
    }
}

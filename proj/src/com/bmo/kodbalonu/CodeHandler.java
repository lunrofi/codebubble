package com.bmo.kodbalonu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

/** Algılanan bir kod için yapılacakların tamamı. */
public final class CodeHandler {
    private static final String CHANNEL = "kodbalonu_kod";
    private static int sNotifId = 100;

    private CodeHandler() {}

    public static void handle(Context context, String code, String source,
            String snippet, String ignoreType, String ignoreValue) {
        if (context == null || code == null) return;
        Context app = context.getApplicationContext();
        SharedPreferences prefs =
                app.getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        boolean autoCopy = prefs.getBoolean("auto_copy", true);
        boolean bubble = prefs.getBoolean("bubble", true);
        boolean notif = prefs.getBoolean("notif", true);
        boolean toast = prefs.getBoolean("toast", false);
        boolean broadcast = prefs.getBoolean("broadcast", false);
        boolean sensitive = prefs.getBoolean("sensitive_clip", true);

        HistoryStore.add(app, source, code, snippet);

        if (autoCopy) {
            ClipboardHelper.copy(app, code, sensitive);
        }
        if (toast) {
            Context wrapped = LangHelper.wrap(app);
            Toast.makeText(wrapped,
                    wrapped.getString(R.string.toast_code, code),
                    Toast.LENGTH_LONG).show();
        }
        if (bubble) {
            BubbleManager.show(app, code);
        }
        if (broadcast) {
            try {
                Intent b = new Intent("com.bmo.kodbalonu.CODE_DETECTED");
                b.putExtra("code", code);
                b.putExtra("source", source);
                app.sendBroadcast(b);
            } catch (Exception ignored) {
            }
        }
        if (notif) {
            showNotification(app, code, source, ignoreType, ignoreValue);
        }
    }

    private static void showNotification(Context app, String code, String source,
            String ignoreType, String ignoreValue) {
        try {
            Context w = LangHelper.wrap(app);
            NotificationManager nm = (NotificationManager)
                    app.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL, w.getString(R.string.notif_channel),
                        NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(app, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPi = PendingIntent.getActivity(app, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            Intent copy = new Intent(app, CopyReceiver.class);
            copy.putExtra("code", code);
            PendingIntent copyPi = PendingIntent.getBroadcast(app,
                    code.hashCode(), copy,
                    PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE);

            Notification.Builder b;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = new Notification.Builder(app, CHANNEL);
            } else {
                b = new Notification.Builder(app);
            }
            b.setContentTitle(w.getString(R.string.notif_title, code))
                    .setContentText(source)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(openPi)
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.ic_menu_set_as,
                            w.getString(R.string.action_copy), copyPi);
            if (ignoreType != null && ignoreValue != null) {
                Intent ign = new Intent(app, IgnoreReceiver.class);
                ign.putExtra("type", ignoreType);
                ign.putExtra("value", ignoreValue);
                PendingIntent ignPi = PendingIntent.getBroadcast(app,
                        ignoreValue.hashCode(), ign,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);
                b.addAction(android.R.drawable.ic_delete,
                        w.getString(R.string.action_ignore), ignPi);
            }
            nm.notify(sNotifId++, b.build());
        } catch (Exception ignored) {
        }
    }
}

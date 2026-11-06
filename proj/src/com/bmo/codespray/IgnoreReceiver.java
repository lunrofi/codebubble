package com.bmo.codespray;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

/** Bildirimdeki "Yoksay" eylemi: uygulamayı veya göndereni listeye ekler. */
public class IgnoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String type = intent.getStringExtra("type");
        String value = intent.getStringExtra("value");
        if (type == null || value == null || value.isEmpty()) return;
        String key = "app".equals(type) ? "ignored_apps" : "ignored_senders";
        SharedPreferences prefs =
                context.getSharedPreferences("codespray", Context.MODE_PRIVATE);
        String old = prefs.getString(key, "");
        if (!old.contains(value)) {
            String updated = old.isEmpty() ? value : old + "\n" + value;
            prefs.edit().putString(key, updated).apply();
        }
        Context w = LangHelper.wrap(context);
        Toast.makeText(w, w.getString(R.string.toast_ignored, value),
                Toast.LENGTH_SHORT).show();
    }
}

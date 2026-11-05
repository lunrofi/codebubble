package com.bmo.codebubble;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Bildirimdeki "Kopyala" eylemi: kodu yeniden panoya koyar. */
public class CopyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String code = intent.getStringExtra("code");
        if (code == null) return;
        try {
            boolean sensitive = context
                    .getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                    .getBoolean("sensitive_clip", true);
            ClipboardHelper.copy(context, code, sensitive);
            Context w = LangHelper.wrap(context);
            Toast.makeText(w, w.getString(R.string.toast_copied, code),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }
}

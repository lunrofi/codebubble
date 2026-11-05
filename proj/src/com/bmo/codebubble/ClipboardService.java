package com.bmo.codebubble;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Panoyu izleyen ön plan servisi.
 *
 * Not: Yeni Android sürümlerinde arka plandaki bir uygulama panoyu
 * her zaman okuyamayabilir. Bu yüzden servis hem dinleyici hem de
 * 1 saniyelik yoklama kullanır; yine de baloncuk çıkmazsa uygulamadaki
 * "Erişilebilirlik" düğmesiyle isteğe bağlı erişilebilirlik servisi
 * açılabilir, o servis pano okuma konusunda muaf tutulur.
 */
public class ClipboardService extends Service
        implements ClipboardManager.OnPrimaryClipChangedListener {
    public static final String ACTION_SHOW_TEST = "com.bmo.codebubble.SHOW_TEST";
    public static final String EXTRA_CODE = "code";
    private static final String CHANNEL = "codebubble";
    private static final int NOTIF_ID = 1;

    private ClipboardManager mClipboard;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String mLastClipText;
    private String mLastShownCode;

    private final Runnable mPoll = new Runnable() {
        @Override public void run() {
            checkClipboard();
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "CodeBubble", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Pano izleniyor, kod gelince baloncuk çıkar.");
            nm.createNotificationChannel(ch);
        }
        Context w = LangHelper.wrap(this);
        Notification notif = new Notification.Builder(this, CHANNEL)
                .setContentTitle(w.getString(R.string.clipboard_running_title))
                .setContentText(w.getString(R.string.clipboard_running_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, notif);

        mClipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (mClipboard != null) {
            mClipboard.addPrimaryClipChangedListener(this);
        }
        mHandler.post(mPoll);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_SHOW_TEST.equals(intent.getAction())) {
            String code = intent.getStringExtra(EXTRA_CODE);
            if (code != null) BubbleManager.show(this, code);
        }
        return START_STICKY;
    }

    @Override
    public void onPrimaryClipChanged() {
        checkClipboard();
    }

    private void checkClipboard() {
        if (mClipboard == null || !mClipboard.hasPrimaryClip()) return;
        ClipData clip = mClipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).getText();
        if (text == null) return;
        String s = text.toString();
        if (s.equals(mLastClipText)) return;
        mLastClipText = s;
        String code = CodeExtractor.extract(s);
        if (code == null) return;
        if (code.equals(mLastShownCode)) return;
        mLastShownCode = code;
        BubbleManager.show(this, code);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mPoll);
        if (mClipboard != null) {
            mClipboard.removePrimaryClipChangedListener(this);
        }
        BubbleManager.hide(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

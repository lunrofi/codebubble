package com.bmo.codespray;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

/**
 * İsteğe bağlı erişilebilirlik servisi.
 *
 * Yalnızca panoyu yoklar; pencere içeriğini okumaz
 * (canRetrieveWindowContent kapalıdır). Bazı Android sürümlerinde
 * ön plan servisi panoyu arka planda okuyamadığı için bu servis
 * yedek yol olarak sunulur. Kullanıcı Ayarlar > Erişilebilirlik
 * bölümünden kendisi açmadıkça çalışmaz.
 */
public class ClipboardAccessibilityService extends AccessibilityService {
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
    protected void onServiceConnected() {
        super.onServiceConnected();
        mHandler.post(mPoll);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        checkClipboard();
    }

    @Override
    public void onInterrupt() {
        mHandler.removeCallbacks(mPoll);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mPoll);
        super.onDestroy();
    }

    private void checkClipboard() {
        ClipboardManager cm =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData clip = cm.getPrimaryClip();
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
}

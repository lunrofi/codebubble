package com.bmo.kodbalonu;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Ekranda beliren sürüklenebilir kod baloncuğu.
 *
 * Kullanım:
 * - Baloncuğu hemen sürüklemek baloncuğu taşır (alanı kapatıyorsa kenara al).
 * - Kodun üstüne uzun basmak sistem genelinde sürüklemeyi başlatır;
 *   yazı alanının üstünde bırakınca alan kodu alır (alan kabul ederse).
 * - Koda dokunmak kodu yeniden panoya kopyalar, baloncuk kalır.
 * - X baloncuğu kapatır. 90 saniye sonra baloncuk kendiliğinden kapanır.
 *
 * "Diğer uygulamaların üzerinde göster" izni yoksa hiçbir şey yapmaz,
 * mevcut kopyalama davranışı aynen sürer.
 */
public final class BubbleManager {
    private static WindowManager sWm;
    private static View sBubble;
    private static WindowManager.LayoutParams sParams;
    private static String sCode;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Runnable TIMEOUT = new Runnable() {
        @Override public void run() { hideInternal(); }
    };

    private BubbleManager() {}

    public static void show(Context context, String code) {
        if (context == null || code == null) return;
        final Context app = context.getApplicationContext();
        MAIN.post(new Runnable() {
            @Override public void run() { showInternal(app, code); }
        });
    }

    public static void hide(Context context) {
        MAIN.post(new Runnable() {
            @Override public void run() { hideInternal(); }
        });
    }

    private static void showInternal(Context ctx, String code) {
        if (!Settings.canDrawOverlays(ctx)) return;
        if (sBubble != null) {
            // Zaten açık baloncuk varsa kodu güncelle, süreyi sıfırla.
            sCode = code;
            TextView tv = (TextView) sBubble.getTag();
            if (tv != null) tv.setText(code);
            MAIN.removeCallbacks(TIMEOUT);
            MAIN.postDelayed(TIMEOUT, 90_000);
            return;
        }
        sCode = code;
        sWm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        if (sWm == null) return;

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1F2937);
        bg.setCornerRadius(48f);
        root.setBackground(bg);
        int pad = dp(ctx, 14);
        root.setPadding(pad, dp(ctx, 10), pad, dp(ctx, 10));
        root.setElevation(dp(ctx, 8));

        TextView codeView = new TextView(ctx);
        codeView.setText(code);
        codeView.setTextColor(Color.WHITE);
        codeView.setTextSize(22);
        codeView.setTypeface(null, android.graphics.Typeface.BOLD);
        codeView.setPadding(0, 0, dp(ctx, 12), 0);
        root.addView(codeView);
        root.setTag(codeView);

        TextView close = new TextView(ctx);
        close.setText("×");
        close.setTextColor(0xFF9CA3AF);
        close.setTextSize(22);
        close.setPadding(dp(ctx, 4), 0, dp(ctx, 2), 0);
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { hideInternal(); }
        });
        root.addView(close);

        // Koda dokun: yeniden kopyala. Uzun bas: alana sürükle-bırak.
        codeView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean sensitive = ctx
                        .getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                        .getBoolean("sensitive_clip", true);
                ClipboardHelper.copy(ctx, sCode, sensitive);
                Context w = LangHelper.wrap(ctx);
                Toast.makeText(w, w.getString(R.string.toast_copied, sCode),
                        Toast.LENGTH_SHORT).show();
            }
        });
        codeView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                ClipData data = ClipData.newPlainText("Kod", sCode);
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                return v.startDragAndDrop(data, shadow, null, View.DRAG_FLAG_GLOBAL);
            }
        });
        codeView.setOnDragListener(new View.OnDragListener() {
            @Override public boolean onDrag(View v, DragEvent event) {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    // Bırakma başarılıysa baloncuk görevini yaptı.
                    if (event.getResult()) hideInternal();
                }
                return true;
            }
        });

        // Baloncuğu taşımak için sürükleme. Hem kök hem kod görünümünde
        // olmalı, çünkü dokunma kodun üstündeyse olay önce kod görünümüne
        // gider; kökteki dinleyiciye ulaşmaz.
        View.OnTouchListener moveListener = new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved;

            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = sParams.x;
                        startY = sParams.y;
                        moved = false;
                        return false;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = e.getRawX() - downX;
                        float dy = e.getRawY() - downY;
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) moved = true;
                        // Her zaman false dönülür: böylece görünümün kendi
                        // dokunma işlemi de çalışır ve hareket büyüyünce
                        // uzun-bas beklemesi kendiliğinden iptal olur.
                        // Yoksa baloncuk taşınırken yanlışlıkla sistem
                        // sürüklemesi de başlayabilirdi.
                        if (moved && sWm != null && sBubble != null) {
                            sParams.x = startX + (int) dx;
                            sParams.y = startY + (int) dy;
                            sWm.updateViewLayout(sBubble, sParams);
                        }
                        return false;
                    }
                    default:
                        return false;
                }
            }
        };
        root.setOnTouchListener(moveListener);
        codeView.setOnTouchListener(moveListener);

        sParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        sParams.gravity = Gravity.TOP | Gravity.START;
        sParams.x = dp(ctx, 80);
        sParams.y = dp(ctx, 260);
        sBubble = root;
        try {
            sWm.addView(root, sParams);
        } catch (Exception e) {
            sBubble = null;
            sWm = null;
            return;
        }
        MAIN.removeCallbacks(TIMEOUT);
        MAIN.postDelayed(TIMEOUT, 90_000);
    }

    private static void hideInternal() {
        MAIN.removeCallbacks(TIMEOUT);
        if (sWm != null && sBubble != null) {
            try {
                sWm.removeView(sBubble);
            } catch (Exception ignored) {
            }
        }
        sBubble = null;
        sWm = null;
        sCode = null;
    }

    private static int dp(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}

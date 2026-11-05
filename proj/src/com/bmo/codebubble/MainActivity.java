package com.bmo.codebubble;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.List;

/**
 * CodeBubble 3.1 — redesigned home screen.
 * Header with icon, colored section cards with icons, switches,
 * colored status rows. Same features as 3.0, new look.
 */
public class MainActivity extends Activity {
    private static final int REQ_SMS = 1;
    private static final int REQ_NOTIF = 2;
    private static final String[] LANG_CODES = {
            "en", "system", "tr", "es", "de", "fr", "it", "pt", "ru", "ar",
            "hi", "id", "zh"
    };
    private static final String[] LANG_NAMES = {
            "English", null, "Türkçe", "Español", "Deutsch", "Français",
            "Italiano", "Português", "Русский", "العربية", "हिन्दी",
            "Bahasa Indonesia", "中文"
    };

    private LinearLayout mStatusBox;
    private java.util.Map<String, TextView> mSectionSubtitles =
            new java.util.HashMap<>();
    private LinearLayout mHistoryBox;
    private LinearLayout mPermContent;
    private LinearLayout mExtraPermContent;
    private Button mTopStartBtn;
    private android.os.Handler mTestHandler;
    private Runnable mTestRunnable;
    private EditText mPhrasesEdit;
    private EditText mIgnoredPhrasesEdit;
    private EditText mCleanupEdit;
    private EditText mIgnoredAppsEdit;
    private EditText mIgnoredSendersEdit;
    private boolean mSpinnerInit = true;
    // ---- Theme (light / dark) ----
    private boolean mDark;
    private int C_BG, C_CARD, C_TEXT, C_SUB, C_DIV, C_EDIT_BG, C_EDIT_BORDER,
            C_CMD_BG, C_SEC_TEXT;

    private void initTheme() {
        mDark = getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .getString("theme", "dark").equals("dark");
        if (mDark) {
            C_BG = 0xFF050508;
            C_CARD = 0xFF14141E;
            C_TEXT = 0xFFE4E6EB;
            C_SUB = 0xFF9CA3AF;
            C_DIV = 0xFF2A2A35;
            C_EDIT_BG = 0xFF1E2025;
            C_EDIT_BORDER = 0xFF33363F;
            C_CMD_BG = 0xFF1E2025;
            C_SEC_TEXT = 0xFF64B5F6;
        } else {
            C_BG = 0xFFF6F8FB;
            C_CARD = Color.WHITE;
            C_TEXT = 0xFF111827;
            C_SUB = 0xFF4B5563;
            C_DIV = 0xFFE5E7EB;
            C_EDIT_BG = 0xFFF9FAFB;
            C_EDIT_BORDER = 0xFFE5E7EB;
            C_CMD_BG = 0xFFF3F4F6;
            C_SEC_TEXT = 0xFF1E88E5;
        }
    }

    private android.graphics.drawable.Drawable rippleBg(
            GradientDrawable content, int rippleColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(rippleColor),
                    content, null);
        }
        return content;
    }

    private void pressAnim(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View vv,
                    android.view.MotionEvent e) {
                switch (e.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        vv.animate().scaleX(0.96f).scaleY(0.96f)
                                .setDuration(100).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        vv.animate().scaleX(1f).scaleY(1f)
                                .setDuration(100).start();
                        break;
                }
                return false;
            }
        });
    }


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTheme();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(mDark ? 0xFF050508 : C_BG);
        }
        View decor = getWindow().getDecorView();
        int vis = decor.getSystemUiVisibility();
        if (mDark) {
            vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(vis);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(bgGlow());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);

        // ---- Header: blue gradient, icon, title, tagline ----
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackground(glassBg());
        header.setPadding(dp(20), dp(20), dp(20), dp(20));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.setMargins(dp(16), dp(16), dp(16), 0);
        header.setLayoutParams(headerLp);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                dp(60), dp(60));
        iconLp.setMargins(0, 0, dp(16), 0);
        icon.setLayoutParams(iconLp);
        header.addView(icon);
        LinearLayout headerText = new LinearLayout(this);
        headerText.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextSize(30);
        title.setTextColor(C_TEXT);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.addView(title);
        TextView tagline = new TextView(this);
        tagline.setText(getString(R.string.app_tagline));
        tagline.setTextSize(14);
        tagline.setTextColor(C_SUB);
        headerText.addView(tagline);
        TextView version = new TextView(this);
        version.setText("v3.11");
        version.setTextSize(12);
        version.setTextColor(C_SUB);
        headerText.addView(version);
        header.addView(headerText);
        View headerSpacer = new View(this);
        headerSpacer.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(headerSpacer);
        TextView themeBtn = new TextView(this);
        themeBtn.setText(mDark ? "\u2600" : "\u263E");
        themeBtn.setTextSize(28);
        themeBtn.setTextColor(C_TEXT);
        themeBtn.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable themeMask = new GradientDrawable();
        themeMask.setShape(GradientDrawable.OVAL);
        themeMask.setColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            themeBtn.setBackground(
                    new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(
                                    mDark ? 0x40FFFFFF : 0x1A000000),
                            null, themeMask));
        }
        pressAnim(themeBtn);
        themeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                        .edit().putString("theme", mDark ? "light" : "dark")
                        .apply();
                recreate();
            }
        });
        header.addView(themeBtn);
        root.addView(header);
        root.addView(spacer(dp(16)));

        // ---- Start tracking (top, prominent) ----
        mTopStartBtn = primaryButton(getString(R.string.btn_start_clip),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startTracking();
                    }
                });
        mTopStartBtn.setTextSize(18);
        mTopStartBtn.setMinHeight(dp(64));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(dp(16), 0, dp(16), dp(14));
        mTopStartBtn.setLayoutParams(topLp);
        root.addView(mTopStartBtn);

        // ---- Language ----
        LinearLayout langCard = section(root, 0xFF7C3AED,
                android.R.drawable.ic_menu_sort_alphabetically,
                getString(R.string.section_language), "language", false, "");
        Spinner spinner = new Spinner(this);
        LANG_NAMES[1] = getString(R.string.lang_system);
        final int spinText = C_TEXT;
        final int spinCard = C_CARD;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, LANG_NAMES) {
            @Override public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                ((TextView) v).setTextColor(spinText);
                return v;
            }

            @Override public View getDropDownView(int pos, View cv,
                    ViewGroup parent) {
                View v = super.getDropDownView(pos, cv, parent);
                ((TextView) v).setTextColor(spinText);
                v.setBackgroundColor(spinCard);
                return v;
            }
        };
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String current = LangHelper.getLang(this);
        for (int i = 0; i < LANG_CODES.length; i++) {
            if (LANG_CODES[i].equals(current)) {
                spinner.setSelection(i);
                break;
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v,
                    int pos, long id) {
                if (mSpinnerInit) {
                    mSpinnerInit = false;
                    return;
                }
                String code = LANG_CODES[pos];
                if (!code.equals(LangHelper.getLang(MainActivity.this))) {
                    LangHelper.setLang(MainActivity.this, code);
                    recreate();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> p) {
            }
        });
        langCard.addView(spinner);
        setSectionSubtitle("language",
                LANG_NAMES[spinner.getSelectedItemPosition()]);

        // ---- Status ----
        LinearLayout statusCard = section(root, 0xFF059669,
                android.R.drawable.ic_dialog_info,
                getString(R.string.section_status), "status", true, "");
        mStatusBox = new LinearLayout(this);
        mStatusBox.setOrientation(LinearLayout.VERTICAL);
        statusCard.addView(mStatusBox);

        // ---- Permissions ----
        LinearLayout permCard = section(root, 0xFFD97706,
                android.R.drawable.ic_lock_lock,
                getString(R.string.section_permissions), "permissions", false, "");
        mPermContent = permCard;
        refreshPermissions();

        // ---- Additional permissions (optional) ----
        LinearLayout extraCard = section(root, 0xFF6B7280,
                android.R.drawable.ic_menu_more,
                getString(R.string.section_extra_perms), "extra_perms", false, "");
        mExtraPermContent = extraCard;
        refreshExtraPerms();

        // ---- Settings (switches) ----
        LinearLayout setCard = section(root, 0xFF2563EB,
                android.R.drawable.ic_menu_preferences,
                getString(R.string.section_settings), "settings", false, "");
        addSwitch(setCard, R.string.toggle_auto_copy, "auto_copy", true, false);
        addSwitch(setCard, R.string.toggle_bubble, "bubble", true, false);
        addSwitch(setCard, R.string.toggle_notif, "notif", true, false);
        addSwitch(setCard, R.string.toggle_toast, "toast", false, false);
        addSwitch(setCard, R.string.toggle_broadcast, "broadcast", false, false);
        addSwitch(setCard, R.string.toggle_sensitive_clip, "sensitive_clip",
                true, false);
        addSwitch(setCard, R.string.toggle_mask_history, "mask_history",
                false, true);
        addSwitch(setCard, R.string.toggle_autoclean, "autoclean", false, false);

        // ---- Phrases ----
        LinearLayout phCard = section(root, 0xFF0D9488,
                android.R.drawable.ic_menu_edit,
                getString(R.string.section_phrases), "phrases", false, "");
        TextView phDesc = new TextView(this);
        phDesc.setText(getString(R.string.phrases_desc));
        phDesc.setTextSize(14);
        phDesc.setTextColor(C_SUB);
        phCard.addView(phDesc);
        phCard.addView(spacer(dp(8)));
        mPhrasesEdit = styledEdit();
        mPhrasesEdit.setMinLines(5);
        mPhrasesEdit.setText(joinPhrases());
        phCard.addView(mPhrasesEdit);
        phCard.addView(spacer(dp(10)));
        LinearLayout phRow = new LinearLayout(this);
        phRow.setOrientation(LinearLayout.HORIZONTAL);
        phRow.addView(primaryButton(getString(R.string.btn_save),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.savePhrases(MainActivity.this,
                                mPhrasesEdit.getText().toString());
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        phRow.addView(secondaryButton(getString(R.string.btn_reset),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.resetPhrases(MainActivity.this);
                        mPhrasesEdit.setText(joinPhrases());
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        phCard.addView(phRow);

        // ---- Ignored phrases ----
        LinearLayout igPhCard = section(root, 0xFFDC2626,
                android.R.drawable.ic_delete,
                getString(R.string.section_ignored_phrases),
                "ignored_phrases", false, "");
        TextView igPhDesc = new TextView(this);
        igPhDesc.setText(getString(R.string.ignored_phrases_desc));
        igPhDesc.setTextSize(14);
        igPhDesc.setTextColor(C_SUB);
        igPhCard.addView(igPhDesc);
        igPhCard.addView(spacer(dp(8)));
        mIgnoredPhrasesEdit = styledEdit();
        mIgnoredPhrasesEdit.setMinLines(5);
        mIgnoredPhrasesEdit.setText(
                joinList(Detector.getIgnoredPhrases(this)));
        igPhCard.addView(mIgnoredPhrasesEdit);
        igPhCard.addView(spacer(dp(10)));
        LinearLayout igPhRow = new LinearLayout(this);
        igPhRow.setOrientation(LinearLayout.HORIZONTAL);
        igPhRow.addView(primaryButton(getString(R.string.btn_save),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.saveIgnoredPhrases(MainActivity.this,
                                mIgnoredPhrasesEdit.getText().toString());
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        igPhRow.addView(secondaryButton(getString(R.string.btn_reset),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.resetIgnoredPhrases(MainActivity.this);
                        mIgnoredPhrasesEdit.setText(joinList(
                                Detector.getIgnoredPhrases(
                                        MainActivity.this)));
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        igPhCard.addView(igPhRow);

        // ---- Cleanup phrases ----
        LinearLayout clCard = section(root, 0xFF0E7490,
                android.R.drawable.ic_menu_revert,
                getString(R.string.section_cleanup_phrases),
                "cleanup_phrases", false, "");
        TextView clDesc = new TextView(this);
        clDesc.setText(getString(R.string.cleanup_phrases_desc));
        clDesc.setTextSize(14);
        clDesc.setTextColor(C_SUB);
        clCard.addView(clDesc);
        clCard.addView(spacer(dp(8)));
        mCleanupEdit = styledEdit();
        mCleanupEdit.setMinLines(5);
        mCleanupEdit.setText(
                joinList(Detector.getCleanupPhrases(this)));
        clCard.addView(mCleanupEdit);
        clCard.addView(spacer(dp(10)));
        LinearLayout clRow = new LinearLayout(this);
        clRow.setOrientation(LinearLayout.HORIZONTAL);
        clRow.addView(primaryButton(getString(R.string.btn_save),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.saveCleanupPhrases(MainActivity.this,
                                mCleanupEdit.getText().toString());
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        clRow.addView(secondaryButton(getString(R.string.btn_reset),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Detector.resetCleanupPhrases(MainActivity.this);
                        mCleanupEdit.setText(joinList(
                                Detector.getCleanupPhrases(
                                        MainActivity.this)));
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }), new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        clCard.addView(clRow);

        // ---- Ignored ----
        LinearLayout igCard = section(root, 0xFF6B7280,
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.section_ignored), "ignored", false, "");
        mIgnoredAppsEdit = styledEdit();
        mIgnoredAppsEdit.setHint(getString(R.string.ignored_apps_hint));
        mIgnoredAppsEdit.setMinLines(2);
        mIgnoredAppsEdit.setText(pref("ignored_apps", ""));
        igCard.addView(mIgnoredAppsEdit);
        igCard.addView(spacer(dp(8)));
        mIgnoredSendersEdit = styledEdit();
        mIgnoredSendersEdit.setHint(getString(R.string.ignored_senders_hint));
        mIgnoredSendersEdit.setMinLines(2);
        mIgnoredSendersEdit.setText(pref("ignored_senders", ""));
        igCard.addView(mIgnoredSendersEdit);
        igCard.addView(spacer(dp(10)));
        igCard.addView(primaryButton(getString(R.string.btn_save),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        prefEdit("ignored_apps",
                                mIgnoredAppsEdit.getText().toString());
                        prefEdit("ignored_senders",
                                mIgnoredSendersEdit.getText().toString());
                        Toast.makeText(MainActivity.this,
                                getString(R.string.toast_saved),
                                Toast.LENGTH_SHORT).show();
                    }
                }));

        // ---- Clipboard ----
        LinearLayout clipCard = section(root, 0xFF4F46E5,
                android.R.drawable.ic_menu_save,
                getString(R.string.section_clipboard), "clipboard", false,
                getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                        .getBoolean("enabled", false)
                        ? getString(R.string.value_on)
                        : getString(R.string.value_off));
        clipCard.addView(secondaryButton(getString(R.string.btn_stop_clip),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        setEnabled(false);
                        cancelTestNotification();
                        stopService(new Intent(MainActivity.this,
                                ClipboardService.class));
                        BubbleManager.hide(MainActivity.this);
                        refreshStatus();
                    }
                }));
        clipCard.addView(secondaryButton(getString(R.string.btn_test_bubble),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!Settings.canDrawOverlays(MainActivity.this)) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.toast_need_overlay),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        BubbleManager.show(MainActivity.this, "123456");
                    }
                }));

        // ---- History ----
        LinearLayout histCard = section(root, 0xFFDB2777,
                android.R.drawable.ic_menu_recent_history,
                getString(R.string.section_history), "history", false, "");
        mHistoryBox = new LinearLayout(this);
        mHistoryBox.setOrientation(LinearLayout.VERTICAL);
        histCard.addView(mHistoryBox);
        histCard.addView(spacer(dp(10)));
        histCard.addView(secondaryButton(getString(R.string.btn_clear_history),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        HistoryStore.clear(MainActivity.this);
                        refreshHistory();
                    }
                }));

        TextView note = new TextView(this);
        note.setText(getString(R.string.note_text));
        note.setTextSize(13);
        note.setTextColor(C_SUB);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.setMargins(dp(16), 0, dp(16), dp(24));
        note.setLayoutParams(noteLp);
        root.addView(note);

        setContentView(scroll);
        showDemoIfFirst();
    }

    private void showDemoIfFirst() {
        final SharedPreferences prefs =
                getSharedPreferences("codebubble", Context.MODE_PRIVATE);
        if (prefs.getBoolean("demo_shown", false)) return;
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        VideoView video = new VideoView(this);
        video.setVideoURI(Uri.parse("android.resource://"
                + getPackageName() + "/" + R.raw.demo_drag));
        video.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(500)));
        video.setOnPreparedListener(
                new android.media.MediaPlayer.OnPreparedListener() {
                    @Override public void onPrepared(
                            android.media.MediaPlayer mp) {
                        mp.setLooping(true);
                    }
                });
        box.addView(video);
        box.addView(spacer(dp(12)));
        box.addView(primaryButton(getString(R.string.btn_got_it),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        prefs.edit().putBoolean("demo_shown", true).apply();
                        dialog.dismiss();
                    }
                }));
        dialog.setContentView(box);
        dialog.setCancelable(false);
        dialog.show();
        video.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshPermissions();
        refreshHistory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
        refreshPermissions();
    }

    private View spacer(int height) {
        View s = new View(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height));
        return s;
    }

    private int darker(int c) {
        return Color.rgb((int) (Color.red(c) * 0.65f),
                (int) (Color.green(c) * 0.65f),
                (int) (Color.blue(c) * 0.65f));
    }

    private android.graphics.drawable.Drawable bgGlow() {
        if (!mDark) return new android.graphics.drawable.ColorDrawable(C_BG);
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientCenter(0.5f, 0f);
        g.setGradientRadius(dp(700));
        g.setColors(new int[]{0xFF2A1650, 0xFF0A0A12, 0xFF050508});
        return g;
    }

    private android.graphics.drawable.Drawable glassBg() {
        GradientDrawable border = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                mDark ? new int[]{0xFFA855F7, 0xFF3B82F6, 0xFFEC4899}
                        : new int[]{0xFFC7D2FE, 0xFF93C5FD, 0xFFFBCFE8});
        border.setCornerRadius(dp(28));
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(C_CARD);
        fill.setCornerRadius(dp(26));
        android.graphics.drawable.LayerDrawable ld =
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[]{border, fill});
        ld.setLayerInset(1, dp(2), dp(2), dp(2), dp(2));
        return ld;
    }

    private void setSectionSubtitle(String key, String text) {
        TextView sub = mSectionSubtitles.get(key);
        if (sub == null) return;
        sub.setText(text);
        sub.setVisibility(text == null || text.isEmpty()
                ? View.GONE : View.VISIBLE);
    }

    private LinearLayout card(LinearLayout root) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(glassBg());
        c.setPadding(dp(18), dp(18), dp(18), dp(18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), 0, dp(16), dp(14));
        c.setLayoutParams(lp);
        root.addView(c);
        return c;
    }

    private LinearLayout section(LinearLayout root, int color, int iconRes,
            String title, final String key, boolean defaultOpen,
            String subtitle) {
        final LinearLayout cardLayout = card(root);
        final LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        final SharedPreferences prefs =
                getSharedPreferences("codebubble", Context.MODE_PRIVATE);
        final boolean open = prefs.getBoolean("sec_" + key, defaultOpen);
        content.setVisibility(open ? View.VISIBLE : View.GONE);
        final TextView arrow = new TextView(this);
        arrow.setText("\u203A");
        arrow.setRotation(open ? 90 : 0);
        arrow.setTextSize(24);
        arrow.setTextColor(C_SUB);
        LinearLayout header = sectionHeader(color, iconRes, title, arrow,
                subtitle);
        LinearLayout textCol = (LinearLayout) header.getChildAt(1);
        mSectionSubtitles.put(key, (TextView) textCol.getChildAt(1));
        header.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean nowOpen = content.getVisibility() != View.VISIBLE;
                android.transition.TransitionManager.beginDelayedTransition(
                        cardLayout, new android.transition.AutoTransition());
                content.setVisibility(nowOpen ? View.VISIBLE : View.GONE);
                arrow.animate().rotation(nowOpen ? 90 : 0)
                        .setDuration(200).start();
                prefs.edit().putBoolean("sec_" + key, nowOpen).apply();
            }
        });
        cardLayout.addView(header);
        cardLayout.addView(content);
        return content;
    }

    private LinearLayout sectionHeader(int color, int iconRes, String title,
            TextView arrow, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{color, darker(color)});
        bg.setShape(GradientDrawable.OVAL);
        box.setBackground(bg);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                dp(48), dp(48));
        boxLp.setMargins(0, 0, dp(12), 0);
        box.setLayoutParams(boxLp);
        ImageView iv = new ImageView(this);
        iv.setImageResource(iconRes);
        iv.setColorFilter(Color.WHITE);
        box.addView(iv, new LinearLayout.LayoutParams(dp(24), dp(24)));
        row.addView(box);
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(C_TEXT);
        textCol.addView(t);
        TextView sub = new TextView(this);
        sub.setText(subtitle == null ? "" : subtitle);
        sub.setTextSize(13);
        sub.setTextColor(C_SUB);
        sub.setVisibility(subtitle == null || subtitle.isEmpty()
                ? View.GONE : View.VISIBLE);
        textCol.addView(sub);
        row.addView(textCol);
        row.addView(arrow);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowLp);
        return row;
    }

    private Button styledButton(String text, boolean primary,
            View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setMinHeight(dp(52));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(16));
        if (primary) {
            bg.setColor(0xFF1E88E5);
            b.setTextColor(Color.WHITE);
            b.setBackground(rippleBg(bg, 0x40FFFFFF));
        } else {
            bg.setColor(C_CARD);
            bg.setStroke(dp(2), 0xFF1E88E5);
            b.setTextColor(C_SEC_TEXT);
            b.setBackground(rippleBg(bg, 0x331E88E5));
        }
        pressAnim(b);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private Button primaryButton(String text, View.OnClickListener l) {
        return styledButton(text, true, l);
    }

    private Button secondaryButton(String text, View.OnClickListener l) {
        return styledButton(text, false, l);
    }

    private EditText styledEdit() {
        EditText e = new EditText(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_EDIT_BG);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), C_EDIT_BORDER);
        e.setBackground(bg);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setTextSize(15);
        e.setTextColor(C_TEXT);
        e.setHintTextColor(C_SUB);
        return e;
    }

    private void addSwitch(LinearLayout root, int labelRes, final String key,
            boolean def, final boolean refreshHist) {
        final SharedPreferences prefs =
                getSharedPreferences("codebubble", Context.MODE_PRIVATE);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText(labelRes);
        label.setTextSize(16);
        label.setTextColor(C_TEXT);
        label.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);
        final Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                prefs.edit().putBoolean(key, c).apply();
                if (refreshHist) refreshHistory();
            }
        });
        row.addView(sw);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                sw.toggle();
            }
        });
        root.addView(row);
        root.addView(divider());
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(C_DIV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0, dp(10), 0, dp(10));
        d.setLayoutParams(lp);
        return d;
    }

    private String joinPhrases() {
        return joinList(Detector.getPhrases(this));
    }

    private String joinList(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private String pref(String key, String def) {
        return getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .getString(key, def);
    }

    private void prefEdit(String key, String value) {
        getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }

    private void setEnabled(boolean enabled) {
        getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .edit().putBoolean("enabled", enabled).apply();
    }

    private boolean mandatoryAll() {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                        == PackageManager.PERMISSION_GRANTED
                && isPostNotifGranted()
                && isNotifAccessEnabled()
                && Settings.canDrawOverlays(this)
                && isBgUnrestricted();
    }

    private void refreshTopButton() {
        if (mTopStartBtn == null) return;
        boolean enabled = getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .getBoolean("enabled", false);
        if (enabled) {
            mTopStartBtn.setText(getString(R.string.tracking_active));
            mTopStartBtn.setEnabled(false);
            mTopStartBtn.setAlpha(1f);
        } else {
            mTopStartBtn.setText(getString(R.string.btn_start_clip));
            boolean ok = mandatoryAll();
            mTopStartBtn.setEnabled(ok);
            mTopStartBtn.setAlpha(ok ? 1f : 0.45f);
        }
    }

    private void startTracking() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, getString(R.string.toast_need_overlay),
                    Toast.LENGTH_LONG).show();
            return;
        }
        setEnabled(true);
        Intent svc = new Intent(this, ClipboardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        refreshStatus();
        refreshPermissions();
        showReadyDialog();
        scheduleTestNotification();
    }

    private void showReadyDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_CARD);
        bg.setCornerRadius(dp(20));
        box.setBackground(bg);
        TextView t = new TextView(this);
        t.setText(getString(R.string.ready_title));
        t.setTextSize(20);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(C_TEXT);
        box.addView(t);
        box.addView(spacer(dp(10)));
        TextView msg = new TextView(this);
        msg.setText(getString(R.string.ready_msg));
        msg.setTextSize(15);
        msg.setTextColor(C_SUB);
        box.addView(msg);
        box.addView(spacer(dp(14)));
        box.addView(primaryButton(getString(R.string.btn_got_it),
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                    }
                }));
        dialog.setContentView(box);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }

    private void scheduleTestNotification() {
        cancelTestNotification();
        if (mTestHandler == null) mTestHandler = new android.os.Handler();
        mTestRunnable = new Runnable() {
            @Override public void run() {
                postTestNotification();
            }
        };
        mTestHandler.postDelayed(mTestRunnable, 10000);
    }

    private void cancelTestNotification() {
        if (mTestHandler != null && mTestRunnable != null) {
            mTestHandler.removeCallbacks(mTestRunnable);
        }
        mTestRunnable = null;
    }

    private void postTestNotification() {
        try {
            Context app = getApplicationContext();
            Context w = LangHelper.wrap(app);
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) app.getSystemService(
                            Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.app.NotificationChannel ch =
                        new android.app.NotificationChannel("codebubble_test",
                                w.getString(R.string.test_notif_title),
                                android.app.NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(app, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            android.app.PendingIntent pi =
                    android.app.PendingIntent.getActivity(app, 0, open,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                    | android.app.PendingIntent.FLAG_IMMUTABLE);
            android.app.Notification.Builder b;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                b = new android.app.Notification.Builder(app, "codebubble_test");
            } else {
                b = new android.app.Notification.Builder(app);
            }
            b.setContentTitle(w.getString(R.string.test_notif_title))
                    .setContentText(w.getString(R.string.test_notif_text))
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            nm.notify(999, b.build());
        } catch (Exception ignored) {
        }
    }

    private void addStatusRow(String label, boolean ok, boolean isOnOff) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = new TextView(this);
        dot.setText("●");
        dot.setTextSize(14);
        dot.setTextColor(ok ? 0xFF059669 : 0xFFDC2626);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dotLp.setMargins(0, 0, dp(10), 0);
        dot.setLayoutParams(dotLp);
        row.addView(dot);
        TextView lab = new TextView(this);
        lab.setText(label);
        lab.setTextSize(15);
        lab.setTextColor(C_TEXT);
        lab.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lab);
        TextView val = new TextView(this);
        if (isOnOff) {
            val.setText(ok ? getString(R.string.value_on)
                    : getString(R.string.value_off));
        } else {
            val.setText(ok ? getString(R.string.value_yes)
                    : getString(R.string.value_no));
        }
        val.setTextSize(15);
        val.setTypeface(null, android.graphics.Typeface.BOLD);
        val.setTextColor(ok ? 0xFF059669 : 0xFFDC2626);
        row.addView(val);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(4), 0, dp(4));
        row.setLayoutParams(rowLp);
        mStatusBox.addView(row);
    }

    private Button smallButton(String text, boolean primary,
            boolean enabled, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(null, android.graphics.Typeface.BOLD);
        b.setMinHeight(dp(40));
        b.setPadding(dp(16), 0, dp(16), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (primary) {
            bg.setColor(0xFF1E88E5);
            b.setTextColor(Color.WHITE);
            b.setBackground(rippleBg(bg, 0x40FFFFFF));
        } else {
            bg.setColor(mDark ? 0xFF3A3B3C : 0xFFE5E7EB);
            b.setTextColor(mDark ? 0xFFE4E6EB : 0xFF374151);
            b.setBackground(rippleBg(bg, 0x33000000));
        }
        pressAnim(b);
        b.setEnabled(enabled);
        b.setOnClickListener(l);
        return b;
    }

    private void permRow(LinearLayout root, String label, Boolean checked,
            String btnText, boolean btnPrimary, boolean btnEnabled,
            View.OnClickListener l) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView ind = new TextView(this);
        if (checked == null) {
            ind.setText("ⓘ");
            ind.setTextColor(C_SUB);
        } else if (checked) {
            ind.setText("☑");
            ind.setTextColor(0xFF059669);
        } else {
            ind.setText("☐");
            ind.setTextColor(0xFF9CA3AF);
        }
        ind.setTextSize(22);
        LinearLayout.LayoutParams indLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        indLp.setMargins(0, 0, dp(10), 0);
        ind.setLayoutParams(indLp);
        row.addView(ind);
        TextView lab = new TextView(this);
        lab.setText(label);
        lab.setTextSize(15);
        lab.setTextColor(C_TEXT);
        lab.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lab);
        row.addView(smallButton(btnText, btnPrimary, btnEnabled, l));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, dp(6), 0, dp(6));
        row.setLayoutParams(rowLp);
        root.addView(row);
    }

    private boolean isPostNotifGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isBgUnrestricted() {
        android.os.PowerManager pm = (android.os.PowerManager)
                getSystemService(Context.POWER_SERVICE);
        if (pm == null) return false;
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private Intent oemBgIntent() {
        String brand = android.os.Build.MANUFACTURER == null
                ? "" : android.os.Build.MANUFACTURER.toLowerCase();
        try {
            if (brand.contains("samsung")) {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"));
                if (i.resolveActivity(getPackageManager()) != null) return i;
            } else if (brand.contains("xiaomi")) {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                if (i.resolveActivity(getPackageManager()) != null) return i;
            } else if (brand.contains("huawei")) {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                if (i.resolveActivity(getPackageManager()) != null) return i;
            } else if (brand.contains("oppo")) {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                if (i.resolveActivity(getPackageManager()) != null) return i;
            } else if (brand.contains("vivo")) {
                Intent i = new Intent();
                i.setComponent(new android.content.ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                if (i.resolveActivity(getPackageManager()) != null) return i;
            }
        } catch (Exception e) {
            // fall through to generic pages
        }
        return new Intent(
                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
    }

    private void refreshPermissions() {
        if (mPermContent == null) return;
        mPermContent.removeAllViews();
        final boolean sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        permRow(mPermContent, getString(R.string.perm_sms), sms,
                getString(R.string.btn_request), !sms, !sms,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        requestPermissions(new String[]{
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS}, REQ_SMS);
                    }
                });
        final boolean postNotif = isPostNotifGranted();
        permRow(mPermContent, getString(R.string.perm_post_notif), postNotif,
                getString(R.string.btn_request), !postNotif, !postNotif,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (Build.VERSION.SDK_INT
                                >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissions(new String[]{
                                    Manifest.permission.POST_NOTIFICATIONS},
                                    REQ_NOTIF);
                        }
                    }
                });
        permRow(mPermContent, getString(R.string.perm_read_notif),
                isNotifAccessEnabled(), getString(R.string.btn_open), true,
                true, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(new Intent(
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        permRow(mPermContent, getString(R.string.perm_overlay_row),
                Settings.canDrawOverlays(this),
                getString(R.string.btn_open), true, true,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                });
        permRow(mPermContent, getString(R.string.perm_background),
                isBgUnrestricted(), getString(R.string.btn_open), true, true,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(new Intent(
                                Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    }
                });
        if (Build.VERSION.SDK_INT >= 35) {
            TextView note = new TextView(this);
            note.setText(getString(R.string.note_a15));
            note.setTextSize(13);
            note.setTextColor(C_SUB);
            LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            noteLp.setMargins(0, dp(12), 0, dp(8));
            note.setLayoutParams(noteLp);
            mPermContent.addView(note);
            final String[] cmds = {
                    "adb shell cmd appops set --user 0 "
                            + getPackageName()
                            + " RECEIVE_SENSITIVE_NOTIFICATIONS allow",
                    "adb shell am force-stop " + getPackageName()
            };
            for (final String cmd : cmds) {
                LinearLayout cmdRow = new LinearLayout(this);
                cmdRow.setOrientation(LinearLayout.HORIZONTAL);
                cmdRow.setGravity(Gravity.CENTER_VERTICAL);
                TextView cmdText = new TextView(this);
                cmdText.setText(cmd);
                cmdText.setTextSize(12);
                cmdText.setTypeface(android.graphics.Typeface.MONOSPACE);
                cmdText.setTextColor(C_TEXT);
                cmdText.setSingleLine(true);
                cmdText.setEllipsize(android.text.TextUtils.TruncateAt.END);
                GradientDrawable cmdBg = new GradientDrawable();
                cmdBg.setColor(C_CMD_BG);
                cmdBg.setCornerRadius(dp(10));
                cmdText.setBackground(cmdBg);
                cmdText.setPadding(dp(10), dp(10), dp(10), dp(10));
                cmdText.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                cmdRow.addView(cmdText);
                cmdRow.addView(smallButton(getString(R.string.btn_copy),
                        true, true, new View.OnClickListener() {
                            @Override public void onClick(View v) {
                                ClipboardHelper.copy(MainActivity.this, cmd,
                                        false);
                                Toast.makeText(MainActivity.this,
                                        getString(R.string.btn_copy) + " \u2713",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }));
                LinearLayout.LayoutParams cmdLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                cmdLp.setMargins(0, dp(4), 0, dp(4));
                mPermContent.addView(cmdRow);
            }
        }
        int pok = (sms ? 1 : 0) + (postNotif ? 1 : 0)
                + (isNotifAccessEnabled() ? 1 : 0)
                + (Settings.canDrawOverlays(this) ? 1 : 0)
                + (isBgUnrestricted() ? 1 : 0);
        setSectionSubtitle("permissions", pok + "/5");
        refreshExtraPerms();
        refreshTopButton();
    }

    private void refreshExtraPerms() {
        if (mExtraPermContent == null) return;
        mExtraPermContent.removeAllViews();
        String brand = android.os.Build.MANUFACTURER == null
                ? "" : android.os.Build.MANUFACTURER;
        String brandCap = brand.isEmpty() ? ""
                : brand.substring(0, 1).toUpperCase()
                        + brand.substring(1).toLowerCase();
        String oemTitle = brandCap.isEmpty()
                ? getString(R.string.perm_oem_bg)
                : brandCap + " \u2022 " + getString(R.string.perm_oem_bg);
        permRow(mExtraPermContent, oemTitle, null, getString(R.string.btn_open),
                true, true, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try {
                            startActivity(oemBgIntent());
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:"
                                                + getPackageName())));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                });
        permRow(mExtraPermContent, getString(R.string.perm_restricted), null,
                getString(R.string.btn_open), true, true,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                });
    }

    private void refreshStatus() {
        if (mStatusBox == null) return;
        mStatusBox.removeAllViews();
        boolean sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean overlay = Settings.canDrawOverlays(this);
        boolean notifAccess = isNotifAccessEnabled();
        boolean enabled = getSharedPreferences("codebubble", Context.MODE_PRIVATE)
                .getBoolean("enabled", false);
        addStatusRow(getString(R.string.btn_sms_perm), sms, false);
        addStatusRow(getString(R.string.btn_notif_access), notifAccess, false);
        addStatusRow(getString(R.string.btn_overlay), overlay, false);
        addStatusRow(getString(R.string.section_clipboard), enabled, true);
        int ok = (sms ? 1 : 0) + (notifAccess ? 1 : 0) + (overlay ? 1 : 0)
                + (enabled ? 1 : 0);
        setSectionSubtitle("status", ok + "/4");
        setSectionSubtitle("clipboard", enabled
                ? getString(R.string.value_on)
                : getString(R.string.value_off));
        refreshTopButton();
    }

    private boolean isNotifAccessEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (enabled == null) return false;
        return enabled.contains(getPackageName());
    }

    private void refreshHistory() {
        if (mHistoryBox == null) return;
        mHistoryBox.removeAllViews();
        final boolean mask = getSharedPreferences("codebubble",
                Context.MODE_PRIVATE).getBoolean("mask_history", false);
        final boolean sensitive = getSharedPreferences("codebubble",
                Context.MODE_PRIVATE).getBoolean("sensitive_clip", true);
        List<HistoryStore.Entry> entries = HistoryStore.getEntries(this);
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.history_empty));
            empty.setTextSize(15);
            empty.setTextColor(C_SUB);
            mHistoryBox.addView(empty);
            return;
        }
        for (final HistoryStore.Entry e : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(C_EDIT_BG);
            bg.setCornerRadius(dp(14));
            row.setBackground(bg);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(rowLp);
            TextView meta = new TextView(this);
            meta.setText(e.time + " • " + e.source);
            meta.setTextSize(12);
            meta.setTextColor(C_SUB);
            row.addView(meta);
            TextView code = new TextView(this);
            code.setText(mask ? "••••••" : e.code);
            code.setTextSize(20);
            code.setTypeface(null, android.graphics.Typeface.BOLD);
            code.setTextColor(C_SEC_TEXT);
            row.addView(code);
            if (e.snippet != null && !e.snippet.isEmpty()) {
                TextView snip = new TextView(this);
                snip.setText(e.snippet);
                snip.setTextSize(13);
                snip.setTextColor(C_SUB);
                row.addView(snip);
            }
            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            Button copy = new Button(this);
            copy.setText(getString(R.string.history_copy));
            copy.setAllCaps(false);
            copy.setTextColor(C_SEC_TEXT);
            GradientDrawable copyMask = new GradientDrawable();
            copyMask.setCornerRadius(dp(10));
            copyMask.setColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                copy.setBackground(
                        new android.graphics.drawable.RippleDrawable(
                                android.content.res.ColorStateList.valueOf(
                                        0x331E88E5), null, copyMask));
            } else {
                copy.setBackgroundColor(Color.TRANSPARENT);
            }
            pressAnim(copy);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    ClipboardHelper.copy(MainActivity.this, e.code, sensitive);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.toast_copied, e.code),
                            Toast.LENGTH_SHORT).show();
                }
            });
            btnRow.addView(copy);
            Button del = new Button(this);
            del.setText(getString(R.string.history_delete));
            del.setAllCaps(false);
            del.setTextColor(0xFFDC2626);
            GradientDrawable delMask = new GradientDrawable();
            delMask.setCornerRadius(dp(10));
            delMask.setColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                del.setBackground(
                        new android.graphics.drawable.RippleDrawable(
                                android.content.res.ColorStateList.valueOf(
                                        0x33DC2626), null, delMask));
            } else {
                del.setBackgroundColor(Color.TRANSPARENT);
            }
            pressAnim(del);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    HistoryStore.delete(MainActivity.this, e);
                    refreshHistory();
                }
            });
            btnRow.addView(del);
            row.addView(btnRow);
            mHistoryBox.addView(row);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}

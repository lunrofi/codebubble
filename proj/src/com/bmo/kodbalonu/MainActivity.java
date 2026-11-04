package com.bmo.kodbalonu;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * KodBalonu 3.0 — written from scratch.
 * Catches codes from SMS and app notifications, copies them,
 * shows a drag-and-drop bubble, keeps history.
 * Main language: English. 12 languages, "System" follows the phone.
 */
public class MainActivity extends Activity {
    private static final int REQ_SMS = 1;
    private static final String[] LANG_CODES = {
            "en", "system", "tr", "es", "de", "fr", "it", "pt", "ru", "ar",
            "hi", "id", "zh"
    };
    private static final String[] LANG_NAMES = {
            "English", null, "Türkçe", "Español", "Deutsch", "Français",
            "Italiano", "Português", "Русский", "العربية", "हिन्दी",
            "Bahasa Indonesia", "中文"
    };

    private TextView mStatus;
    private LinearLayout mHistoryBox;
    private EditText mPhrasesEdit;
    private EditText mIgnoredAppsEdit;
    private EditText mIgnoredSendersEdit;
    private boolean mSpinnerInit = true;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF2F4F7);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        // Header card
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor(0xFF1E88E5);
        headerBg.setCornerRadius(dp(20));
        header.setBackground(headerBg);
        header.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView title = new TextView(this);
        title.setText("KodBalonu");
        title.setTextSize(28);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);
        TextView desc = new TextView(this);
        desc.setText(getString(R.string.app_desc));
        desc.setTextSize(15);
        desc.setTextColor(Color.WHITE);
        header.addView(desc);
        root.addView(header);
        space(root);

        // Language card
        LinearLayout langCard = card(root);
        langCard.addView(sectionTitle(getString(R.string.section_language)));
        Spinner spinner = new Spinner(this);
        LANG_NAMES[1] = getString(R.string.lang_system);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, LANG_NAMES);
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

        // Status card
        LinearLayout statusCard = card(root);
        statusCard.addView(sectionTitle(getString(R.string.section_status)));
        mStatus = new TextView(this);
        mStatus.setTextSize(15);
        statusCard.addView(mStatus);

        // Permissions card
        LinearLayout permCard = card(root);
        permCard.addView(sectionTitle(getString(R.string.section_permissions)));
        permCard.addView(button(getString(R.string.btn_sms_perm), false,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        requestPermissions(new String[]{
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS}, REQ_SMS);
                    }
                }));
        permCard.addView(button(getString(R.string.btn_notif_access), false,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        startActivity(new Intent(
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                }));
        permCard.addView(button(getString(R.string.btn_overlay), false,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }
                }));

        // Settings card
        LinearLayout setCard = card(root);
        setCard.addView(sectionTitle(getString(R.string.section_settings)));
        addToggle(setCard, R.string.toggle_auto_copy, "auto_copy", true);
        addToggle(setCard, R.string.toggle_bubble, "bubble", true);
        addToggle(setCard, R.string.toggle_notif, "notif", true);
        addToggle(setCard, R.string.toggle_toast, "toast", false);
        addToggle(setCard, R.string.toggle_broadcast, "broadcast", false);
        addToggle(setCard, R.string.toggle_sensitive_clip, "sensitive_clip", true);
        addToggle(setCard, R.string.toggle_mask_history, "mask_history", false);
        addToggle(setCard, R.string.toggle_autoclean, "autoclean", false);

        // Phrases card
        LinearLayout phCard = card(root);
        phCard.addView(sectionTitle(getString(R.string.section_phrases)));
        TextView phDesc = new TextView(this);
        phDesc.setText(getString(R.string.phrases_desc));
        phDesc.setTextSize(14);
        phCard.addView(phDesc);
        mPhrasesEdit = new EditText(this);
        mPhrasesEdit.setMinLines(5);
        mPhrasesEdit.setText(joinPhrases());
        phCard.addView(mPhrasesEdit);
        LinearLayout phRow = new LinearLayout(this);
        phRow.setOrientation(LinearLayout.HORIZONTAL);
        phRow.addView(button(getString(R.string.btn_save), true,
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
        phRow.addView(button(getString(R.string.btn_reset), false,
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

        // Ignored card
        LinearLayout igCard = card(root);
        igCard.addView(sectionTitle(getString(R.string.section_ignored)));
        mIgnoredAppsEdit = new EditText(this);
        mIgnoredAppsEdit.setHint(getString(R.string.ignored_apps_hint));
        mIgnoredAppsEdit.setMinLines(2);
        mIgnoredAppsEdit.setText(pref("ignored_apps", ""));
        igCard.addView(mIgnoredAppsEdit);
        mIgnoredSendersEdit = new EditText(this);
        mIgnoredSendersEdit.setHint(getString(R.string.ignored_senders_hint));
        mIgnoredSendersEdit.setMinLines(2);
        mIgnoredSendersEdit.setText(pref("ignored_senders", ""));
        igCard.addView(mIgnoredSendersEdit);
        igCard.addView(button(getString(R.string.btn_save), true,
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

        // Clipboard card
        LinearLayout clipCard = card(root);
        clipCard.addView(sectionTitle(getString(R.string.section_clipboard)));
        clipCard.addView(button(getString(R.string.btn_start_clip), true,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!Settings.canDrawOverlays(MainActivity.this)) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.toast_need_overlay),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        setEnabled(true);
                        Intent svc = new Intent(MainActivity.this,
                                ClipboardService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(svc);
                        } else {
                            startService(svc);
                        }
                        refreshStatus();
                    }
                }));
        clipCard.addView(button(getString(R.string.btn_stop_clip), false,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        setEnabled(false);
                        stopService(new Intent(MainActivity.this,
                                ClipboardService.class));
                        BubbleManager.hide(MainActivity.this);
                        refreshStatus();
                    }
                }));
        clipCard.addView(button(getString(R.string.btn_test_bubble), false,
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

        // History card
        LinearLayout histCard = card(root);
        histCard.addView(sectionTitle(getString(R.string.section_history)));
        mHistoryBox = new LinearLayout(this);
        mHistoryBox.setOrientation(LinearLayout.VERTICAL);
        histCard.addView(mHistoryBox);
        histCard.addView(button(getString(R.string.btn_clear_history), false,
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        HistoryStore.clear(MainActivity.this);
                        refreshHistory();
                    }
                }));

        TextView note = new TextView(this);
        note.setText(getString(R.string.note_text));
        note.setTextSize(13);
        note.setTextColor(0xFF6B7280);
        root.addView(note);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshHistory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatus();
    }

    private LinearLayout card(LinearLayout root) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setElevation(dp(2));
        }
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.addView(c);
        space(root);
        return c;
    }

    private void space(LinearLayout root) {
        View s = new View(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(12)));
        root.addView(s);
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(18);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setTextColor(0xFF111827);
        return t;
    }

    private Button button(String text, boolean primary,
            View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        if (primary) {
            bg.setColor(0xFF1E88E5);
            b.setTextColor(Color.WHITE);
        } else {
            bg.setColor(0xFFE5E7EB);
            b.setTextColor(0xFF111827);
        }
        b.setBackground(bg);
        b.setOnClickListener(l);
        return b;
    }

    private void addToggle(LinearLayout root, int labelRes, final String key,
            boolean def) {
        final SharedPreferences prefs =
                getSharedPreferences("kodbalonu", Context.MODE_PRIVATE);
        CheckBox cb = new CheckBox(this);
        cb.setText(labelRes);
        cb.setChecked(prefs.getBoolean(key, def));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean c) {
                prefs.edit().putBoolean(key, c).apply();
                if ("mask_history".equals(key)) refreshHistory();
            }
        });
        root.addView(cb);
    }

    private String joinPhrases() {
        StringBuilder sb = new StringBuilder();
        List<String> phrases = Detector.getPhrases(this);
        for (int i = 0; i < phrases.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(phrases.get(i));
        }
        return sb.toString();
    }

    private String pref(String key, String def) {
        return getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .getString(key, def);
    }

    private void prefEdit(String key, String value) {
        getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }

    private void setEnabled(boolean enabled) {
        getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .edit().putBoolean("enabled", enabled).apply();
    }

    private void refreshStatus() {
        boolean sms = checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
        boolean overlay = Settings.canDrawOverlays(this);
        boolean notifAccess = isNotifAccessEnabled();
        boolean enabled = getSharedPreferences("kodbalonu", Context.MODE_PRIVATE)
                .getBoolean("enabled", false);
        String yes = getString(R.string.value_yes);
        String no = getString(R.string.value_no);
        String on = getString(R.string.value_on);
        String off = getString(R.string.value_off);
        mStatus.setText(
                getString(R.string.status_line1, sms ? yes : no,
                        notifAccess ? yes : no)
                        + "\n"
                        + getString(R.string.status_line2, overlay ? yes : no,
                                enabled ? on : off));
    }

    private boolean isNotifAccessEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (enabled == null) return false;
        return enabled.contains(getPackageName());
    }

    private void refreshHistory() {
        mHistoryBox.removeAllViews();
        final boolean mask = getSharedPreferences("kodbalonu",
                Context.MODE_PRIVATE).getBoolean("mask_history", false);
        final boolean sensitive = getSharedPreferences("kodbalonu",
                Context.MODE_PRIVATE).getBoolean("sensitive_clip", true);
        List<HistoryStore.Entry> entries = HistoryStore.getEntries(this);
        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(getString(R.string.history_empty));
            empty.setTextSize(15);
            mHistoryBox.addView(empty);
            return;
        }
        for (final HistoryStore.Entry e : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            TextView tv = new TextView(this);
            String codeText = mask ? "••••••" : e.code;
            String line = e.time + " • " + e.source + "\n" + codeText;
            if (e.snippet != null && !e.snippet.isEmpty()) {
                line += "\n" + e.snippet;
            }
            tv.setText(line);
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tv);
            Button copy = new Button(this);
            copy.setText(getString(R.string.history_copy));
            copy.setAllCaps(false);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    ClipboardHelper.copy(MainActivity.this, e.code, sensitive);
                    Toast.makeText(MainActivity.this,
                            getString(R.string.toast_copied, e.code),
                            Toast.LENGTH_SHORT).show();
                }
            });
            row.addView(copy);
            Button del = new Button(this);
            del.setText(getString(R.string.history_delete));
            del.setAllCaps(false);
            del.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    HistoryStore.delete(MainActivity.this, e);
                    refreshHistory();
                }
            });
            row.addView(del);
            mHistoryBox.addView(row);
        }
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}

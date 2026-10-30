package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A remote-friendly gate. Its owner never inflates settings before successful verification. */
final class SettingsGate {
    private final Activity activity;
    private final Runnable unlocked;
    private TextView digits, message;
    private String entered = "";
    boolean open;

    SettingsGate(Activity activity, Runnable unlocked) { this.activity = activity; this.unlocked = unlocked; }

    void show() {
        entered = "";
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(0xFF101419);
        TextView title = text("设置验证", 24);
        root.addView(title);
        message = text("输入 Authenticator 的六位验证码 · 按返回取消", 14);
        root.addView(message);
        digits = text("○ ○ ○ ○ ○ ○", 26);
        root.addView(digits);
        Button first = null;
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "删除", "0", "确认"};
        float density = activity.getResources().getDisplayMetrics().density;
        for (int row = 0; row < 4; row++) {
            LinearLayout line = new LinearLayout(activity);
            line.setGravity(Gravity.CENTER);
            root.addView(line);
            for (int column = 0; column < 3; column++) {
                String key = keys[row * 3 + column];
                Button button = new Button(activity);
                button.setText(key);
                button.setTextSize(18);
                button.setOnClickListener(v -> press(key));
                line.addView(button, new LinearLayout.LayoutParams((int) (100 * density), (int) (44 * density)));
                if (first == null) first = button;
            }
        }
        activity.setContentView(root);
        if (SettingsAuth.keyFile(activity).length() != 20) message.setText("尚未绑定验证器，请通过电脑完成绑定");
        first.requestFocus();
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(activity);
        view.setText(value); view.setTextSize(size); view.setTextColor(Color.WHITE); view.setGravity(Gravity.CENTER);
        return view;
    }

    private void press(String key) {
        if ("确认".equals(key)) {
            if (entered.length() != 6) { message.setText("请输入六位数字"); return; }
            String error = SettingsAuth.verify(activity, entered);
            entered = "";
            if (error == null) { open = true; unlocked.run(); return; }
            message.setText(error);
        } else if ("删除".equals(key)) {
            if (!entered.isEmpty()) entered = entered.substring(0, entered.length() - 1);
        } else if (entered.length() < 6) entered += key;
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < 6; i++) display.append(i < entered.length() ? "● " : "○ ");
        digits.setText(display.toString().trim());
    }

    boolean key(KeyEvent event) {
        int code = event.getKeyCode();
        String key = code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9 ? String.valueOf(code - KeyEvent.KEYCODE_0)
                : code == KeyEvent.KEYCODE_DEL ? "删除" : code == KeyEvent.KEYCODE_ENTER ? "确认" : null;
        if (key == null) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) press(key);
        return true;
    }
}

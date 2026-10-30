package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/** Private, non-backed-up provisioning key; independent of all theme and layout preferences. */
final class SettingsAuth {
    static File keyFile(Context context) { return new File(context.getNoBackupFilesDir(), "settings-auth.key"); }

    static synchronized String verify(Context context, String code) {
        try {
            File file = keyFile(context);
            if (file.length() != 20) return "尚未绑定验证器，请通过电脑完成绑定";
            byte[] secret = new byte[20];
            try (java.io.DataInputStream in = new java.io.DataInputStream(new FileInputStream(file))) {
                in.readFully(secret);
            }
            String identity = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(secret), Base64.NO_WRAP);
            SharedPreferences state = context.getSharedPreferences("settings_auth", Context.MODE_PRIVATE);
            if (!identity.equals(state.getString("identity", ""))) {
                if (!state.edit().clear().putString("identity", identity).commit()) return "无法保存验证状态";
            }
            long now = System.currentTimeMillis();
            long wait = state.getLong("blocked_until", 0) - now;
            if (wait > 0) return "请等待 " + ((wait + 999) / 1000) + " 秒后重试";
            long match = Totp.match(secret, code, now / 1000, state.getLong("last_step", -1));
            if (match >= 0) {
                if (!state.edit().putLong("last_step", match).putInt("failures", 0)
                        .remove("blocked_until").putBoolean("bound", true).commit()) return "无法保存验证状态";
                return null;
            }
            int failures = state.getInt("failures", 0) + 1;
            SharedPreferences.Editor edit = state.edit().putInt("failures", failures >= 5 ? 0 : failures);
            if (failures >= 5) edit.putLong("blocked_until", now + 60000);
            if (!edit.commit()) return "无法保存验证状态";
            return failures >= 5 ? "连续错误，请等待 60 秒后重试" : "验证码无效或已使用，请查看最新验证码并检查设备时间";
        } catch (Exception e) {
            // Never turn an unreadable key, clock problem or crypto failure into an unlocked screen.
            return "无法验证，请通过电脑检查绑定";
        }
    }

    private SettingsAuth() {}
}

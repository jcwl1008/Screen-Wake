package hk.jimmy.screenwake;

import android.content.SharedPreferences;
import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordStore {
    private final SharedPreferences prefs;
    PasswordStore(SharedPreferences prefs) { this.prefs = prefs; }
    boolean exists() { return prefs.contains("hash") && prefs.contains("salt"); }
    private byte[] derive(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 210000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    void save(char[] password) throws Exception {
        byte[] salt = new byte[24]; new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt);
        if (!prefs.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt("failures", 0).putLong("blockedUntil", 0).commit()) throw new Exception("Storage failed");
    }
    long waitSeconds() { return Math.max(0, (prefs.getLong("blockedUntil", 0) - System.currentTimeMillis() + 999) / 1000); }
    boolean check(char[] password) throws Exception {
        if (!exists() || waitSeconds() > 0) return false;
        byte[] salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP);
        byte[] expected = Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP);
        boolean ok = MessageDigest.isEqual(expected, derive(password, salt));
        int failures = ok ? 0 : prefs.getInt("failures", 0) + 1;
        prefs.edit().putInt("failures", failures >= 5 ? 0 : failures)
            .putLong("blockedUntil", failures >= 5 ? System.currentTimeMillis() + 30000 : 0).commit();
        return ok;
    }
}

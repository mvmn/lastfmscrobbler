package x.mvmn.lastfmscrobbler;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.Preferences;

import x.mvmn.lastfmscrobbler.util.EncryptionUtil;
import x.mvmn.lastfmscrobbler.util.EncryptionUtil.KeyAndNonce;

public class AppSettings {
	private static final String DEFAULT_KEY = "9e89b44de1ff37c5246ad0af18406454";
	private static final String DEFAULT_SECRET = "147320ea9b8930fe196a4231da50ada4";

	private static final String KEY_ENCPWD = "scrobblerencpwd";
	private static final String KEY_USER = "lastfmusername";
	private static final String KEY_PASS = "lastfmpassword";
	private static final String KEY_LASTFMAPIKEY = "lastfmapikey";
	private static final String KEY_LASTFMAPISECRET = "lastfmapisecret";

	private final Preferences prefs;
	private final KeyAndNonce keyAndNonce;

	public AppSettings() throws NoSuchAlgorithmException {
		this.prefs = getPreferences();
		String encpwd = prefs.get(KEY_ENCPWD, null);
		if (encpwd == null) {
			encpwd = EncryptionUtil.generateKeyAndNonce().serialize();
			prefs.put(KEY_ENCPWD, encpwd);
		}
		this.keyAndNonce = KeyAndNonce.deserialize(encpwd);
	}

	public String getUsername() {
		return prefs.get(KEY_USER, null);
	}

	public void setUsername(String value) {
		prefs.put(KEY_USER, value);
	}

	public String getPassword() throws GeneralSecurityException {
		String password = prefs.get(KEY_PASS, null);

		if (password != null) {
			password = EncryptionUtil.decrypt(password, keyAndNonce);
		}
		return password;
	}

	public void setPassword(String value) throws GeneralSecurityException {
		String password = EncryptionUtil.encrypt(value, keyAndNonce);
		prefs.put(KEY_PASS, password);
	}

	public String getApiKey() {
		return prefs.get(KEY_LASTFMAPIKEY, DEFAULT_KEY);
	}

	public void setApiKey(String value) {
		prefs.put(KEY_LASTFMAPIKEY, value);
	}

	public String getApiSecret() {
		return prefs.get(KEY_LASTFMAPISECRET, DEFAULT_SECRET);
	}

	public void setApiSecret(String value) {
		prefs.put(KEY_LASTFMAPISECRET, value);
	}

	protected Preferences getPreferences() {
		return Preferences.userNodeForPackage(LastFMScrobbler.class);
	}
}

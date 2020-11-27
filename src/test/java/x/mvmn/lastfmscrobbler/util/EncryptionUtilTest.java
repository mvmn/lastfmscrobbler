package x.mvmn.lastfmscrobbler.util;

import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Test;

import x.mvmn.lastfmscrobbler.util.EncryptionUtil.KeyAndNonce;

public class EncryptionUtilTest {

	@Test
	public void testGenerateEncryptDecrypt() throws GeneralSecurityException {
		KeyAndNonce k = EncryptionUtil.generateKeyAndNonce();
		byte[] key = k.getKey().getEncoded();
		byte[] nonce = k.getNonce();
		String serialized = k.serialize();
		Assert.assertNotNull(serialized);
		k = KeyAndNonce.deserialize(serialized);
		Assert.assertNotNull(k);
		Assert.assertArrayEquals(key, k.getKey().getEncoded());
		Assert.assertArrayEquals(nonce, k.getNonce());
		Assert.assertEquals("mcguffin", EncryptionUtil.decrypt(EncryptionUtil.encrypt("mcguffin", k), k));
	}
}

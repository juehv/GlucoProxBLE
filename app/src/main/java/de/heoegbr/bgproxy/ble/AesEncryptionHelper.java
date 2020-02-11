package de.heoegbr.bgproxy.ble;

import android.util.Log;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AesEncryptionHelper {

    private static final String TAG = "AES_HELPER";

    private static final String KEY_CONSTRUCTION_ALGORITHM = "PBKDF2WithHmacSHA1";
    // I chose by this recommendation https://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256
    private static final int KEY_PASSWORD_ITERATIONS = 100000;
    private static final int KEY_SIZE = 128;
    private static final String KEY_SALT = "Wfcr78WFrd2r4F";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static String KEY_PASSWORD_CACHE = "";
    private static byte[] KEY_INSTANCE = new byte[0];

    public static byte[] encrypt(String password, byte[] payloadToEncrypt) {
        try {
            if (KEY_INSTANCE.length == 0 || !KEY_PASSWORD_CACHE.equals(password)) {
                KEY_PASSWORD_CACHE = password;
                KEY_INSTANCE = generateKey(password, KEY_SALT);
            }

            SecretKeySpec skeySpec = new SecretKeySpec(KEY_INSTANCE, "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(payloadToEncrypt);
            return encrypted;
        } catch (Exception ex) {
            // pokemon style (catch them all)
            Log.w(TAG, "Encryptin failed!!\n" + ex.toString());
            return payloadToEncrypt;
        }
    }

    private static byte[] generateKey(String plainPassword, String salt) throws Exception {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_CONSTRUCTION_ALGORITHM);
            KeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), salt.getBytes(), KEY_PASSWORD_ITERATIONS, KEY_SIZE);
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            Log.w(TAG, ex.toString());
            throw ex;
        }
    }
}

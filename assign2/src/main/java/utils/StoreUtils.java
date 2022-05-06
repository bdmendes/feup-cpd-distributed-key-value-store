package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StoreUtils {
    public static String sha256(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes);
            byte[] digest = messageDigest.digest();
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : digest) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}

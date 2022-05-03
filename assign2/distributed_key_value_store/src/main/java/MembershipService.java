import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

class MembershipService {
    static String sha256(String str) throws java.security.NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest(str.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
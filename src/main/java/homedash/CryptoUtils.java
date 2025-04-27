package homedash;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;

public class CryptoUtils {

    private static final String ALGO = "AES";
    private static final byte[] keyValue = "1234567890123456".getBytes(); // 16 bytes

    private static Key generateKey() {
        return new SecretKeySpec(keyValue, ALGO);
    }

    public static void encryptFile(File inputFile, File outputFile) throws Exception {
        doCrypto(Cipher.ENCRYPT_MODE, inputFile, outputFile);
    }

    public static void decryptFile(File inputFile, File outputFile) throws Exception {
        doCrypto(Cipher.DECRYPT_MODE, inputFile, outputFile);
    }

    private static void doCrypto(int cipherMode, File inputFile, File outputFile) throws Exception {
        Key secretKey = generateKey();
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(cipherMode, secretKey);

        FileInputStream inputStream = new FileInputStream(inputFile);
        byte[] inputBytes = inputStream.readAllBytes();
        inputStream.close();

        byte[] outputBytes = cipher.doFinal(inputBytes);

        FileOutputStream outputStream = new FileOutputStream(outputFile);
        outputStream.write(outputBytes);
        outputStream.close();
    }
}


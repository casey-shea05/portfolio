import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Protocol1Client {

    // Configuration values from config.properties
    private static final String SERVER_IP = "127.0.0.1"; // Localhost
    private static final int SERVER_PORT = 3200;         // From config.properties
    private static final String SECRET_KEY = "ffffffffffffffffffffffffffffffff"; // From config.properties
    private static final String TOKEN = "5HQUUD"; // Must be exactly 6 characters per provided server code

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
            
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            
            // Protocol Step 1: Send connection request.
            // Construct a 26-byte message: "Connect Protocol 1: " (20 bytes) + TOKEN (6 bytes)
            String prefix = "Connect Protocol 1: ";
            String connectionMessage = prefix + TOKEN;
            byte[] connectionBytes = connectionMessage.getBytes("UTF-8");
            if(connectionBytes.length != 26) {
                System.out.println("Error: Connection message must be 26 bytes, but is " + connectionBytes.length);
                socket.close();
                return;
            }
            outStream.write(connectionBytes);
            System.out.println("Sent connection request: " + connectionMessage);
            
            // Set up the cipher using the same method as the server.
            // The server uses the token as the salt in PBKDF2.
            byte[] tokenBytes = TOKEN.getBytes("UTF-8");
            PBEKeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), tokenBytes, 1000000, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] studentKey = skf.generateSecret(spec).getEncoded();
            Key aesKey = new SecretKeySpec(studentKey, "AES");
            
            Cipher encAEScipher = Cipher.getInstance("AES");
            encAEScipher.init(Cipher.ENCRYPT_MODE, aesKey);
            Cipher decAEScipher = Cipher.getInstance("AES");
            decAEScipher.init(Cipher.DECRYPT_MODE, aesKey);
            
            // Protocol Step 2: Receive server nonce {Ns}_Kcs (16 bytes)
            // (Due to PKCS5 padding, a 16-byte nonce encrypts to 32 bytes.)
            byte[] encNs = new byte[32];
            int readBytes = inStream.read(encNs);
            if(readBytes != 32) {
                System.out.println("Error: Expected 32 bytes for server nonce, got " + readBytes);
                socket.close();
                return;
            }
            byte[] serverNonce = decAEScipher.doFinal(encNs);
            System.out.println("Received server nonce (Ns): " + byteArrayToHexString(serverNonce));
            
            // Protocol Step 3: Generate client nonce (Nc) and send {Nc}_Kcs (16 bytes)
            byte[] clientNonce = new byte[16];
            new SecureRandom().nextBytes(clientNonce);
            byte[] encNc = encAEScipher.doFinal(clientNonce);
            outStream.write(encNc);
            System.out.println("Sent client nonce (Nc): " + byteArrayToHexString(clientNonce));
            
            // Compute session key = Ns XOR Nc (16 bytes)
            byte[] sessionKeyBytes = xorBytes(serverNonce, clientNonce);
            SecretKeySpec sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");
            System.out.println("Computed session key: " + byteArrayToHexString(sessionKeyBytes));
            
            // Set up session ciphers (AES, same settings)
            Cipher encSessionCipher = Cipher.getInstance("AES");
            encSessionCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            Cipher decSessionCipher = Cipher.getInstance("AES");
            decSessionCipher.init(Cipher.DECRYPT_MODE, sessionKey);
            
            // Protocol Step 4: Receive {Nc, Ns}_(Ns ⊕ Nc) (ciphertext length is 48 bytes)
            byte[] encMsg4 = new byte[48];
            readFully(inStream, encMsg4);
            byte[] msg4Plain = decSessionCipher.doFinal(encMsg4);
            
            // The plaintext should consist of two nonces:
            // first 16 bytes = Nc, next 16 bytes = Ns.
            byte[] receivedNc = Arrays.copyOfRange(msg4Plain, 0, 16);
            byte[] receivedNs = Arrays.copyOfRange(msg4Plain, 16, 32);
            System.out.println("Received in message 4:");
            System.out.println("  Nc: " + byteArrayToHexString(receivedNc));
            System.out.println("  Ns: " + byteArrayToHexString(receivedNs));
            
            // Verify that the nonces match what we expect.
            if (!Arrays.equals(receivedNc, clientNonce) || !Arrays.equals(receivedNs, serverNonce)) {
                System.out.println("Nonces do not match! Aborting.");
                socket.close();
                return;
            }
            
            /*  Protocol Step 5: Send confirmation {Ns, Nc}_(Ns ⊕ Nc)
            The server expects the first 16 bytes equal its own nonce (Ns) 
            and the next 16 bytes equal the client nonce (Nc) when decrypting
            Since our original message 4 was [clientNonce, serverNonce],
            we need to swap them before encrypting. */
            byte[] confirmationPlain = new byte[32];
            // serverNonce
            System.arraycopy(serverNonce, 0, confirmationPlain, 0, 16);
            // clientNonce
            System.arraycopy(clientNonce, 0, confirmationPlain, 16, 16);
            byte[] confirmation = encSessionCipher.doFinal(confirmationPlain);
            outStream.write(confirmation);
            System.out.println("Sent nonce confirmation (with swapped order).");
            
            // Protocol Step 6: Receive final with encrypted secret (the flag)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            byte[] encSecret = baos.toByteArray();
            byte[] secretMessage = decSessionCipher.doFinal(encSecret);
            System.out.println("Received secret message: " + new String(secretMessage, "UTF-8"));
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Helper method: Read exactly the full length of the provided buffer from an InputStream.
    private static void readFully(InputStream in, byte[] buffer) throws Exception {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read == -1) {
                throw new Exception("Unexpected end of stream");
            }
            offset += read;
        }
    }
    
    // Helper method: Convert a byte array to a hexadecimal string.
    private static String byteArrayToHexString(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            buf.append(String.format("%02x", b));
        }
        return buf.toString();
    }
    
    // Helper method: XOR two byte arrays of equal length.
    private static byte[] xorBytes(byte[] one, byte[] two) {
        if (one.length != two.length) {
            throw new IllegalArgumentException("Arrays must be of equal length");
        }
        byte[] result = new byte[one.length];
        for (int i = 0; i < one.length; i++) {
            result[i] = (byte) (one[i] ^ two[i]);
        }
        return result;
    }
}


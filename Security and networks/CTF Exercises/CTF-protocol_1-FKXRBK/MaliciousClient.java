import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MaliciousClient {

    // Configuration values (adjust as needed)
    private static final String SERVER_IP = "13.41.132.117";
    private static final int SERVER_PORT = 3200;
    // The provided server code expects a 6-character token.
    private static final String TOKEN = "FKXRBK";

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
            
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();
            
            // ----------------------------------------
            // Protocol Step 1: Send connection request.
            // Construct a 26-byte message: "Connect Protocol 1: " (20 bytes) + TOKEN (6 bytes)
            String prefix = "Connect Protocol 1: ";
            String connectionMessage = prefix + TOKEN;
            byte[] connectionBytes = connectionMessage.getBytes("UTF-8");
            outStream.write(connectionBytes);
            System.out.println("Sent connection request: " + connectionMessage);
            
            // ----------------------------------------
            // Protocol Step 2: Receive server nonce {Ns}_Kcs.
            // With PKCS5 padding, a 16-byte nonce encrypts to 32 bytes.
            byte[] encNs = new byte[32];
            inStream.read(encNs);

            // *** Attack Step ***
            // We do NOT decrypt encNs because we don't know Kcs.
            // Instead, we simply replay the exact ciphertext as our client nonce.
            outStream.write(encNs);
            System.out.println("Replayed server nonce as client's nonce.");
            
            // ----------------------------------------
            // Since we replayed the encrypted nonce, when the server decrypts our client nonce,
            // it obtains its own nonce (Ns). Thus, the session key becomes: Ns XOR Ns = 0.
            byte[] sessionKeyBytes = new byte[16]; // 16 zero bytes.
            System.out.println("Forced session key (all zeros): " + byteArrayToHexString(sessionKeyBytes));
            
            // Set up session ciphers using the zero key.
            SecretKeySpec sessionKey = new SecretKeySpec(sessionKeyBytes, "AES");
            Cipher encSessionCipher = Cipher.getInstance("AES");
            encSessionCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
            Cipher decSessionCipher = Cipher.getInstance("AES");
            decSessionCipher.init(Cipher.DECRYPT_MODE, sessionKey);
            
            // ----------------------------------------
            // Protocol Step 4: Receive confirmation message {Nc, Ns}_(sessionKey).
            // The protocol says this ciphertext should be 48 bytes.
            byte[] encMsg4 = new byte[48];
            inStream.read(encMsg4);
            byte[] msg4Plain = decSessionCipher.doFinal(encMsg4);
            
            // The plaintext should consist of two nonces:
            // first 16 bytes = clientNonce (which, due to our replay, equals Ns),
            // next 16 bytes = serverNonce (Ns).
            byte[] receivedNc = Arrays.copyOfRange(msg4Plain, 0, 16);
            byte[] receivedNs = Arrays.copyOfRange(msg4Plain, 16, 32);
            System.out.println("Received confirmation message:");
            System.out.println("  Client nonce: " + byteArrayToHexString(receivedNc));
            System.out.println("  Server nonce: " + byteArrayToHexString(receivedNs));
            
            // Verify that both halves match (they should be equal since Nc was replayed as Ns).
            if (!Arrays.equals(receivedNc, receivedNs)) {
                System.out.println("Nonces do not match! Aborting.");
                socket.close();
                return;
            }
            
            // ----------------------------------------
            // Protocol Step 5: Send confirmation message {Ns, Nc}_(sessionKey).
            // The server expects the first 16 bytes to equal its own nonce and the next 16 bytes to equal the client nonce.
            // Since we forced Nc = Ns, we send [Ns, Ns].
            byte[] confirmationPlain = new byte[32];
            System.arraycopy(receivedNs, 0, confirmationPlain, 0, 16);
            System.arraycopy(receivedNs, 0, confirmationPlain, 16, 16);
            byte[] confirmation = encSessionCipher.doFinal(confirmationPlain);
            outStream.write(confirmation);
            System.out.println("Sent nonce confirmation (with swapped order).");
            
            // ----------------------------------------
            // Protocol Step 6: Receive final encrypted secret (the flag) from the server.
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
    
    // Utility method: Convert a byte array to a hexadecimal string.
    private static String byteArrayToHexString(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            buf.append(String.format("%02x", b));
        }
        return buf.toString();
    }
}

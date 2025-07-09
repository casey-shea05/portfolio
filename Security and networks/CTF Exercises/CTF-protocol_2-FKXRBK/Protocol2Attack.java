import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Protocol2Attack {
    // Diffie–Hellman parameters (identical to the server's)
    static final BigInteger g = new BigInteger("129115595377796797872260754286990587373919932143310995152019820961988539107450691898237693336192317366206087177510922095217647062219921553183876476232430921888985287191036474977937325461650715797148343570627272553218190796724095304058885497484176448065844273193302032730583977829212948191249234100369155852168");
    static final BigInteger p = new BigInteger("165599299559711461271372014575825561168377583182463070194199862059444967049140626852928438236366187571526887969259319366449971919367665844413099962594758448603310339244779450534926105586093307455534702963575018551055314397497631095446414992955062052587163874172731570053362641344616087601787442281135614434639");
    
    // Server connection details
    static final String SERVER_HOST = "13.41.132.117"; // Adjust as needed
    static final int SERVER_PORT = 3201;            // Adjust as needed
    
    // Your exercise token (should be exactly 6 bytes as per protocol)
    static final String TOKEN = "FKXRBK"; // Example token

    public static void main(String[] args) throws Exception {
        // ------------------- SESSION A -------------------
        System.out.println("Starting Session A...");
        Socket socketA = new Socket(SERVER_HOST, SERVER_PORT);
        DataInputStream inA = new DataInputStream(socketA.getInputStream());
        DataOutputStream outA = new DataOutputStream(socketA.getOutputStream());
        
        // Message 1: Send "Connect Protocol 2: <TOKEN>"
        String connectMsg = "Connect Protocol 2: " + TOKEN;
        byte[] connectMsgBytes = connectMsg.getBytes("UTF-8");
        outA.write(connectMsgBytes);
        
        // Perform Diffie–Hellman exchange in Session A
        DHParameterSpec dhSpec = new DHParameterSpec(p, g);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DiffieHellman");
        kpg.initialize(dhSpec);
        KeyPair keyPairA = kpg.generateKeyPair();
        PrivateKey privA = keyPairA.getPrivate();
        PublicKey pubA = keyPairA.getPublic();
        byte[] pubABytes = pubA.getEncoded();
        outA.writeInt(pubABytes.length);
        outA.write(pubABytes);
        
        // Receive server's Diffie–Hellman public key for Session A
        int serverPubLenA = inA.readInt();
        byte[] serverPubBytesA = new byte[serverPubLenA];
        inA.readFully(serverPubBytesA);
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(serverPubBytesA);
        PublicKey serverPubA = keyFactory.generatePublic(x509Spec);
        
        // Compute the shared secret and derive the AES session key for Session A
        KeyAgreement kaA = KeyAgreement.getInstance("DiffieHellman");
        kaA.init(privA);
        kaA.doPhase(serverPubA, true);
        byte[] sharedSecretA = kaA.generateSecret();
        byte[] aesKeyBytesA = new byte[16];
        System.arraycopy(sharedSecretA, 0, aesKeyBytesA, 0, 16);
        SecretKey aesSessionKeyA = new SecretKeySpec(aesKeyBytesA, "AES");
        Cipher encSessionA = Cipher.getInstance("AES");
        encSessionA.init(Cipher.ENCRYPT_MODE, aesSessionKeyA);
        Cipher decSessionA = Cipher.getInstance("AES");
        decSessionA.init(Cipher.DECRYPT_MODE, aesSessionKeyA);
        
        // Message 4 in Session A: Send an arbitrary client nonce (e.g., 1234)
        int clientNonceA = 1234;
        byte[] nonceABytes = BigInteger.valueOf(clientNonceA).toByteArray();
        byte[] msg4A = encSessionA.doFinal(nonceABytes);
        outA.write(msg4A);
        
        // Receive Message 5 in Session A (expecting 32 bytes)
        byte[] msg5A_ct = new byte[32];
        inA.readFully(msg5A_ct);
        byte[] msg5A_plain = decSessionA.doFinal(msg5A_ct);
        // First 16 bytes: AES(Kcs, clientNonceA+1)
        // Next 4 bytes: server nonce (NsA)
        byte[] nsABytes = new byte[4];
        System.arraycopy(msg5A_plain, 16, nsABytes, 0, 4);
        int serverNonceA = new BigInteger(nsABytes).intValue();
        System.out.println("Session A - Server nonce (NsA): " + serverNonceA);
        
        // ------------------- SESSION B -------------------
        System.out.println("Starting Session B...");
        Socket socketB = new Socket(SERVER_HOST, SERVER_PORT);
        DataInputStream inB = new DataInputStream(socketB.getInputStream());
        DataOutputStream outB = new DataOutputStream(socketB.getOutputStream());
        
        // Send Message 1 in Session B
        outB.write(connectMsgBytes);
        
        // Diffie–Hellman exchange in Session B
        KeyPairGenerator kpgB = KeyPairGenerator.getInstance("DiffieHellman");
        kpgB.initialize(dhSpec);
        KeyPair keyPairB = kpgB.generateKeyPair();
        PrivateKey privB = keyPairB.getPrivate();
        PublicKey pubB = keyPairB.getPublic();
        byte[] pubBBytes = pubB.getEncoded();
        outB.writeInt(pubBBytes.length);
        outB.write(pubBBytes);
        
        // Receive server's Diffie–Hellman public key for Session B
        int serverPubLenB = inB.readInt();
        byte[] serverPubBytesB = new byte[serverPubLenB];
        inB.readFully(serverPubBytesB);
        X509EncodedKeySpec x509SpecB = new X509EncodedKeySpec(serverPubBytesB);
        PublicKey serverPubB = keyFactory.generatePublic(x509SpecB);
        
        // Compute the shared secret and derive the AES session key for Session B
        KeyAgreement kaB = KeyAgreement.getInstance("DiffieHellman");
        kaB.init(privB);
        kaB.doPhase(serverPubB, true);
        byte[] sharedSecretB = kaB.generateSecret();
        byte[] aesKeyBytesB = new byte[16];
        System.arraycopy(sharedSecretB, 0, aesKeyBytesB, 0, 16);
        SecretKey aesSessionKeyB = new SecretKeySpec(aesKeyBytesB, "AES");
        Cipher encSessionB = Cipher.getInstance("AES");
        encSessionB.init(Cipher.ENCRYPT_MODE, aesSessionKeyB);
        Cipher decSessionB = Cipher.getInstance("AES");
        decSessionB.init(Cipher.DECRYPT_MODE, aesSessionKeyB);
        
        // Message 4 in Session B: Use NsA (serverNonceA from Session A) as the client nonce
        int clientNonceB = serverNonceA;
        byte[] nonceBBytes = BigInteger.valueOf(clientNonceB).toByteArray();
        byte[] msg4B = encSessionB.doFinal(nonceBBytes);
        outB.write(msg4B);
        
        // Receive Message 5 in Session B (expecting 32 bytes)
        byte[] msg5B_ct = new byte[32];
        inB.readFully(msg5B_ct);
        byte[] msg5B_plain = decSessionB.doFinal(msg5B_ct);
        // In Session B, the first 16 bytes are: AES(Kcs, (clientNonceB+1))
        // Since clientNonceB = NsA, this equals AES(Kcs, (NsA+1))
        byte[] oracleBlock = new byte[16];
        System.arraycopy(msg5B_plain, 0, oracleBlock, 0, 16);
        System.out.println("Extracted oracle block from Session B.");
        
        // ------------------- Replay in Session A -------------------
        // In Session A, the server expects Message 6 to be:
        //   encAESsessionCipher( AES(Kcs, (NsA+1)) )
        // We already have AES(Kcs, (NsA+1)) as oracleBlock from Session B.
        byte[] msg6A = encSessionA.doFinal(oracleBlock);
        outA.write(msg6A);
        
        // Receive Message 7 (the secret) in Session A.
        byte[] msg7_ct = new byte[1024];
        int len = inA.read(msg7_ct);
        byte[] msg7_plain = decSessionA.doFinal(msg7_ct, 0, len);
        String secret = new String(msg7_plain, "UTF-8");
        System.out.println("Secret received: " + secret);
        
        // Close both sessions
        socketA.close();
        socketB.close();
    }
}


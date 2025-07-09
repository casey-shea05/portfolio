//package procotols;

// Code for Protocol Challenge 1 of the Security & Networks module
// Tom Chothia, The University of Birmingham, Sept. 2014
// Dan Clark, The University of Birmingham, Jan. 2025

// This program runs the server side of the following protocol:

// 1. C → S : "Connect Protocol 1: <Token>"
// 2. S → C : {Ns}_Kcs
// 3. C → S : {Nc}_Kcs
// 4. S → C : {Nc, Ns}_(Ns ⊕ Nc)
// 5. C → S : {Ns, Nc}_(Ns ⊕ Nc)
// 6. S → C : {Secret Value}_(Ns ⊕ Nc)


// Encryption is 128-bit AES, ECB, PKCS5 padding, nonces are 128 bits. 

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


public class Protocol1Server {
    
    static String challengeName = "";
    static String secretKey = "";
    static int connCounter = 0;
    

    public static void main (String[] args) throws IOException {
        // Read in the config properties
        int portNumber;
        try (InputStream input = new FileInputStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            portNumber = Integer.parseInt(prop.getProperty("SERVER_PORT"));
            challengeName = prop.getProperty("CHALLENGE_NAME");
            secretKey = prop.getProperty("SECRET_KEY");
        }

        // Listen for connections, when client connects spin off a 
        // thread to run the protocol over that connection and go 
        // back to listening for new connections
        try { 
            ServerSocket listening = new ServerSocket(portNumber);
            System.out.println("Ex3: Protocol 1 - Server started - Listening on port " + portNumber);
            while (true) {
                // For each connection spin off a new protocol instance.
                Socket connection = listening.accept();
                Thread instance = new Thread(new ProtocolInstance(connection));
                instance.start();
            }
        } catch (Exception e) {
            System.out.println("Exception whilst starting threads: " + e);
        }
    }
    
    
    private static class ProtocolInstance implements Runnable {
    
        Socket myConnection;
        int connID;
        static Cipher decAEScipher;
        static Cipher encAEScipher;
        
        public ProtocolInstance(Socket myConnection) {
            this.myConnection = myConnection;
            this.connID = connCounter;
            connCounter++;
        }

        public void run() {
            System.out.println("Conn " + this.connID + " - Connected to: " + myConnection.getRemoteSocketAddress().toString());

            OutputStream outStream;
            InputStream inStream;
            try {
                outStream = myConnection.getOutputStream();
                inStream = myConnection.getInputStream();

                // Protocol Step 1
                // We should be sent the ascii for "Connect Protocol 1: <TOKEN>"
                byte[] message1 = new byte[26];
                inStream.read(message1);
                byte[] token = Arrays.copyOfRange(message1, 20, 26);
                String tokenStr = new String(token, "UTF-8");
                System.out.println("Conn " + this.connID + " - Token: " + tokenStr);
                
                //Set up the cipher object
                try {
                    PBEKeySpec spec = new PBEKeySpec(secretKey.toCharArray(), token, 1000000, 256);
                    SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    byte[] studentKey = skf.generateSecret(spec).getEncoded();

                    Key aesKey = new SecretKeySpec(studentKey, "AES");
                    decAEScipher = Cipher.getInstance("AES");
                    decAEScipher.init(Cipher.DECRYPT_MODE, aesKey);
                    encAEScipher = Cipher.getInstance("AES");
                    encAEScipher.init(Cipher.ENCRYPT_MODE, aesKey);
                } catch (Exception e) {
                    System.out.println("Conn " + this.connID + " - Exception whilst setting up Ciphers: " + e);
                }           
                
                // Protocol Step 2
                // We send the nonce challenge. {Ns}_Kcs
                SecureRandom random = new SecureRandom();
                byte[] serverNonce = new byte[16];
                random.nextBytes(serverNonce);
                byte[] cipherTextM2;
                try {
                    cipherTextM2 = encAEScipher.doFinal(serverNonce);
                    outStream.write(cipherTextM2);
                    
                    //Protocol Step 3
                    byte[] message3 = new byte[32];
                    inStream.read(message3);
                    byte[] clientNonce = decAEScipher.doFinal(message3);
                    
                    // Calculate session key
                    byte[] keyBytes = xorBytes(serverNonce,clientNonce);
                    SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
                    Cipher decAEScipherSession = Cipher.getInstance("AES");         
                    decAEScipherSession.init(Cipher.DECRYPT_MODE, secretKeySpec);
                    Cipher encAEScipherSession = Cipher.getInstance("AES");         
                    encAEScipherSession.init(Cipher.ENCRYPT_MODE, secretKeySpec);
                    System.out.println("Conn " + this.connID + " - Session key: " + byteArrayToHexString(keyBytes));
                    
                    //Protocol Step 4 
                    byte[] message4pt =  new byte[32];
                    System.arraycopy(clientNonce, 0, message4pt, 0, 16);
                    System.arraycopy(serverNonce, 0, message4pt, 16, 16);
                    byte[] cipherTextM4 = encAEScipherSession.doFinal(message4pt);  
                    outStream.write(cipherTextM4);
                    
                    //Protocol Step 5 
                    byte[] cipherTextM5 =  new byte[48];
                    inStream.read(cipherTextM5);
                    byte[] message5pt = decAEScipherSession.doFinal(cipherTextM5);      
                    byte[] inNs = new byte[16];
                    byte[] inNc = new byte[16];
                    System.arraycopy(message5pt, 0, inNs, 0, 16);
                    System.arraycopy(message5pt, 16, inNc, 0, 16);
                    System.out.println("Conn " + this.connID + " - M5 inNc: " + byteArrayToHexString(inNc));
                    System.out.println("Conn " + this.connID + " - M5 inNs: " + byteArrayToHexString(inNs));
                    
                    //Check the challenge values are correct.
                    if (!(Arrays.equals(inNc,clientNonce) && Arrays.equals(inNs,serverNonce))) {
                        System.out.println("Conn " + this.connID + " - Nonces dont match");
                        outStream.write("Nonces dont match".getBytes());
                        return;
                    }
                    
                    //Protocol Step 6
                    String flag = Flags.getFlag(challengeName, tokenStr, this.connID);
                    byte[] plainTextM6 = ("Well Done! The flag is: " + flag).getBytes();
                    byte[] cipherTextM6 = encAEScipherSession.doFinal(plainTextM6);
                    outStream.write(cipherTextM6);
                    System.out.println("Conn " + this.connID + " - " + tokenStr + " got flag: " + flag);

                    myConnection.close();
                
                //Oh, isn't Java fun:   
                } catch (IllegalBlockSizeException e) {
                    outStream.write("Bad block size".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad block size: " + e);
                } catch (BadPaddingException e) {
                    outStream.write("Bad padding".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad padding: " + e);
                } catch (InvalidKeyException e) {
                    outStream.write("Bad Key".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad Key: " + e);
                } catch (NoSuchAlgorithmException e) {
                    // Not going to happen, AES hard wired
                    System.out.println("Conn " + this.connID + " - Exception whilst running protocol: " + e);
                } catch (NoSuchPaddingException e) {
                    // Not going to happen, PKCS5 hard wired
                    System.out.println("Conn " + this.connID + " - Exception whilst running protocol: " + e);
                }
            } catch (IOException e) {
                //Nothing we can do about this one
                System.out.println("Conn " + this.connID + " - See that cable on the back of your computer? Stop pulling it out: " + e);
            }
        }
    }
    
    
    private static byte[] xorBytes (byte[] one, byte[] two) {
        if (one.length!=two.length) {
            return null;
        } else {
            byte[] result = new byte[one.length];
            for(int i=0;i<one.length;i++) {
                result[i] = (byte) (one[i]^two[i]);
            }
            return result;
        }
    }
    
    private static String byteArrayToHexString(byte[] data) { 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
            if ((0 <= halfbyte) && (halfbyte <= 9)) 
                buf.append((char) ('0' + halfbyte));
            else 
                buf.append((char) ('a' + (halfbyte - 10)));
            halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 
    
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                      + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
    
}

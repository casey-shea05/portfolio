//package procotols;

// Code for Protocol Challenge 2 of the Security & Networks module
// Tom Chothia, The University of Birmingham, Sept. 2014
// Dan Clark, The University of Birmingham, Jan. 2025

// This program runs the server side of the following protocol:

// 1. C → S : "Connect Protocol 2: <Token>"
// 2. C → S : g^x
// 3. S → C : g^y
// 4. C → S : {Nc}_g^xy
// 5. S → C : {{Nc + 1}_Kcs, Ns}_g^xy
// 6. C → S : {{Ns + 1}_Kcs}_g^xy
// 7. S → C : {Secret Value}_g^xy


// Encryption is 128-bit AES, CBC, PKCS5 padding, nonces are ints. 
// g^x, and g^y are sent as certificates, length sent as int first.
// The first 128-bits of g^xy are used as the AES key.
// Values of p & g for Diffie-Hellman were found using generateDHprams()

import java.util.Properties;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;

public class Protocol2Server {

    static String challengeName = "";
    static String secretKey = "";
    static int connCounter = 0;
    
    // Values of p & g for Diffie-Hellman found using generateDHprams()
    static BigInteger g = new BigInteger("129115595377796797872260754286990587373919932143310995152019820961988539107450691898237693336192317366206087177510922095217647062219921553183876476232430921888985287191036474977937325461650715797148343570627272553218190796724095304058885497484176448065844273193302032730583977829212948191249234100369155852168");
    static BigInteger p = new BigInteger("165599299559711461271372014575825561168377583182463070194199862059444967049140626852928438236366187571526887969259319366449971919367665844413099962594758448603310339244779450534926105586093307455534702963575018551055314397497631095446414992955062052587163874172731570053362641344616087601787442281135614434639");


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
            System.out.println("Ex3: Protocol 2 - Server started - Listening on port " + portNumber);
            while (true) {
                // For each connection spin off a new protocol instance.
                Socket connection = listening.accept();
                Thread instance = new Thread(new Protocol2Instance(connection));
                instance.start();
            }
        } catch (Exception e) {
            System.out.println("Exception whilst starting threads: " + e);
        }
    }
    
    
    private static class Protocol2Instance implements Runnable {
    
        Socket myConnection;
        int connID;
        Cipher decAEScipher;
        Cipher encAEScipher;
        Cipher decAESsessionCipher;
        Cipher encAESsessionCipher;
        
        public Protocol2Instance(Socket myConnection) {
            this.myConnection = myConnection;
            this.connID = connCounter;
            connCounter++;
        }
        
        public void run() {
            // Data streams used because we want to send bytes and ints
            DataOutputStream outStream;
            DataInputStream inStream;
            try {
                outStream = new DataOutputStream(myConnection.getOutputStream());
                inStream = new DataInputStream(myConnection.getInputStream());
                try {
                    // Use crypto API to calculate y & g^y
                    DHParameterSpec dhSpec = new DHParameterSpec(p, g);
                    KeyPairGenerator diffieHellmanGen = KeyPairGenerator.getInstance("DiffieHellman");
                    diffieHellmanGen.initialize(dhSpec);
                    KeyPair serverPair = diffieHellmanGen.generateKeyPair();
                    PrivateKey y = serverPair.getPrivate();
                    PublicKey gToTheY = serverPair.getPublic();
                    
                    // Protocol message 1
                    // We should be sent the ascii for "Connect Protocol 2: <TOKEN>"
                    byte[] message1 = new byte[26];
                    inStream.read(message1);
                    byte[] token = Arrays.copyOfRange(message1, 20, 26);
                    String tokenStr = new String(token, "UTF-8");
                    System.out.println("Conn " + this.connID + " - Token: " + tokenStr);

                    // Set up the cipher objects
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

                    // Protocol message 2
                    // PublicKey cert can vary in length, therefore the length is sent first
                    int publicKeyLen = inStream.readInt();
                    byte[] message2 = new byte[publicKeyLen];
                    inStream.read(message2);

                    KeyFactory keyfactoryDH = KeyFactory.getInstance("DH");
                    X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(message2);
                    PublicKey gToTheX = keyfactoryDH.generatePublic(x509Spec);
                    System.out.println("Conn " + this.connID + " - g^x len: " + publicKeyLen);
                    System.out.println("Conn " + this.connID + " - g^x cert: " + byteArrayToHexString(gToTheX.getEncoded()));

                    // Protocol message 3
                    outStream.writeInt(gToTheY.getEncoded().length);
                    outStream.write(gToTheY.getEncoded());
                    System.out.println("Conn " + this.connID + " - g^y len: " + gToTheY.getEncoded().length);
                    System.out.println("Conn " + this.connID + " - g^y cert: " + byteArrayToHexString(gToTheY.getEncoded()));
                    
                    // Calculate session key
                    // This method sets decAESsessionCipher & encAESsessionCipher
                    calculateSessionKey(y, gToTheX);
                    
                    // Protocol Step 4
                    byte[] message4ct = new byte[16];
                    inStream.read(message4ct);
                    byte[] clientNonceBytes = decAESsessionCipher.doFinal(message4ct);
                    int clientNonce = new BigInteger(clientNonceBytes).intValue();
                    System.out.println("Conn " + this.connID + " - Client nonce: " + clientNonce);
                    
                    // Protocol Step 5
                    SecureRandom gen = new SecureRandom();
                    int serverNonce = gen.nextInt();
                    byte[] encryptedClientNonceInc = encAEScipher.doFinal(BigInteger.valueOf(clientNonce + 1).toByteArray());
                    byte[] serverNonceBytes = BigInteger.valueOf(serverNonce).toByteArray();
                    byte[] message5body = new byte[20];
                    System.arraycopy(encryptedClientNonceInc, 0, message5body, 0, 16);
                    System.arraycopy(serverNonceBytes, 0, message5body, 16, 4);
                    byte[] message5ct = encAESsessionCipher.doFinal(message5body);
                    outStream.write(message5ct);
                    System.out.println("Conn " + this.connID + " - Server nonce: " + serverNonce);
                    
                    // Protocol Step 6
                    byte[] message6ct = new byte[32];
                    inStream.read(message6ct);
                    byte[] nonceReplyBytes = decAEScipher.doFinal(decAESsessionCipher.doFinal(message6ct));
                    int serverNonceReply = new BigInteger(nonceReplyBytes).intValue();
                    System.out.println("Conn " + this.connID + " - Server Nonce Reply: " + serverNonceReply);
                    
                    // Check nonce value
                    if (serverNonce + 1 != serverNonceReply) {
                        System.out.println("Conn " + this.connID + " - Nonces dont match");
                        outStream.write("Nonces dont match".getBytes());
                        myConnection.close();
                        return;
                    }
                    
                    // Protocol Step 7
                    String flag = Flags.getFlag(challengeName, tokenStr, this.connID);
                    byte[] message7pt = ("Well Done! The flag is: " + flag).getBytes();
                    byte[] message7ct = encAESsessionCipher.doFinal(message7pt);
                    outStream.write(message7ct);
                    System.out.println("Conn " + this.connID + " - " + tokenStr + " got flag: " + flag);

                    myConnection.close();
                    
                } catch (IllegalBlockSizeException e) {
                    outStream.write("Bad block size".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad block size: " + e);
                } catch (BadPaddingException e) {
                    outStream.write("Bad padding".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad padding: " + e);
                } catch (InvalidKeySpecException e) {
                    outStream.write("Bad certificate for PublicKey (g^x)".getBytes());
                    myConnection.close();
                    System.out.println("Conn " + this.connID + " - Bad certificate for PublicKey (g^x): " + e);
                } catch (NoSuchAlgorithmException e) {
                    // Not going to happen, AES hard wired
                    System.out.println("Conn " + this.connID + " - Exception whilst running protocol: " + e);
                } catch (InvalidAlgorithmParameterException e) {
                    // Not going to happen, DH Spec hard wired
                    System.out.println("Conn " + this.connID + " - Exception whilst running protocol: " + e);
                } 
            
            } catch (IOException e) {
                //Nothing we can do about this one
                System.out.println("Conn " + this.connID + " - Your wi-fi sucks: " + e);
            }
        }
    
        // This method sets decAESsessioncipher & encAESsessioncipher 
        private void calculateSessionKey(PrivateKey y, PublicKey gToTheX)  {
            try {
                // Find g^xy
                KeyAgreement serverKeyAgree = KeyAgreement.getInstance("DiffieHellman");
                serverKeyAgree.init(y);
                serverKeyAgree.doPhase(gToTheX, true);
                byte[] secretDH = serverKeyAgree.generateSecret();
                System.out.println("Conn " + this.connID + " - g^xy: " + byteArrayToHexString(secretDH));
                // Use first 16 bytes of g^xy to make an AES key
                byte[] aesSecret = new byte[16];
                System.arraycopy(secretDH, 0, aesSecret, 0, 16);
                Key aesSessionKey = new SecretKeySpec(aesSecret, "AES");
                System.out.println("Conn " + this.connID + " - Session key: " + byteArrayToHexString(aesSessionKey.getEncoded()));
                // Set up Cipher Objects
                decAESsessionCipher = Cipher.getInstance("AES");
                decAESsessionCipher.init(Cipher.DECRYPT_MODE, aesSessionKey);
                encAESsessionCipher = Cipher.getInstance("AES");
                encAESsessionCipher.init(Cipher.ENCRYPT_MODE, aesSessionKey);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Conn " + this.connID + " - Exception whilst calculating session key: " + e);
            } catch (InvalidKeyException e) {
                System.out.println("Conn " + this.connID + " - Exception whilst calculating session key: " + e);
            } catch (NoSuchPaddingException e) {
                System.out.println("Conn " + this.connID + " - Exception whilst calculating session key: " + e);
            }
        }
    
        @SuppressWarnings("unused")
        public static void generateDHprams() throws NoSuchAlgorithmException, InvalidParameterSpecException {
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");   
            paramGen.init(1024);   
            // Generate the parameters   
            AlgorithmParameters params = paramGen.generateParameters();   
            DHParameterSpec dhSpec = (DHParameterSpec) params.getParameterSpec(DHParameterSpec.class);   
            System.out.println("These are some good values to use for p & g with Diffie Hellman");
            System.out.println("p: " + dhSpec.getP());
            System.out.println("g: " + dhSpec.getG());
        }
    
        private static String byteArrayToHexString(byte[] data) { 
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < data.length; i++) { 
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do { 
                    if (0 <= halfbyte && halfbyte <= 9) {
                        buf.append((char) ('0' + halfbyte));
                    } else {
                        buf.append((char) ('a' + (halfbyte - 10)));
                    }
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
}


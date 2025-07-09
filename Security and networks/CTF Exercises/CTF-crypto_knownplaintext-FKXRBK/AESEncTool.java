// An encryption tool

import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class AESEncTool {

    static String inFile = "plainText.txt";
    static String outFile = "cipherText.enc";
    static String hexKey="523ad61692127099664cfc4da2e8605f";
    static String keyStore;
    static String keyName;

    public static void main(String[] args) {

        // Handle the command line arguments
        if (args.length==4 && args[0].equals("-encAESCTR") && args[1].length()==32 ) {
            hexKey = args[1];
            inFile = args[2];
            outFile = args[3];
            encryptAESCTR();
        } else if (args.length==4 && args[0].equals("-decAESCTR") &&args[1].length()==32 ) {
            hexKey = args[1];
            inFile = args[2];
            outFile = args[3];
            decryptAESCTR();
        } else if (args.length==3 && args[0].equals("-modCTR")) {
            inFile = args[1];
            outFile = args[2];
            modifyCtrEnc();
        } else { 
            System.out.println("This is a simple program to encrypt and decrypt files");
            System.out.println("Usage: ");
            System.out.println("    -encAESCTR <key:128 bits in as hex> <inputFile> <outputFile>  AES CTR mode encrypt");
            System.out.println("    -decAESCTR <key:128 bits in as hex> <inputFile> <outputFile>  AES CTR mode decrypt");
            System.out.println("    -modCTR <inputFile> <outputFile>  Modify CTR encrypted file");
        }
    }

    private static void encryptAESCTR() {
        try {
            // Open and read the input file
            // N.B. this program reads the whole file into memory, not good for large programs!
            RandomAccessFile rawDataFromFile = new RandomAccessFile(inFile, "r");
            byte[] plainText = new byte[(int) rawDataFromFile.length()];
            rawDataFromFile.read(plainText);
            rawDataFromFile.close();

            //Set up the AES key & cipher object in CTR mode
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(hexKey), "AES");
            Cipher encAESCTRcipher = Cipher.getInstance("AES/CTR/NoPadding");
            SecureRandom random = new SecureRandom();
            byte iv[] = new byte[16];
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            encAESCTRcipher.init(Cipher.ENCRYPT_MODE, secretKeySpec,ivSpec);

            //Encrypt the data
            byte[] cipherText = encAESCTRcipher.doFinal(plainText);

            //Write file to disk
            System.out.println("Opening file to write: "+outFile);
            FileOutputStream outToFile = new FileOutputStream(outFile);
            outToFile.write(iv);
            outToFile.write(cipherText);
            outToFile.close();
            System.out.println(inFile+" encrypted as "+outFile);
        } catch (Exception e){
            System.out.println("doh "+e);
        }
    }

    private static void decryptAESCTR() {

        try {
            // Open and read the input file
            RandomAccessFile rawDataFromFile = new RandomAccessFile(inFile, "r");
            byte[] iv = new byte[16]; // We read the IV first because when we encrypted it, we wrote it first
            rawDataFromFile.read(iv);
            byte[] cipherText = new byte[(int) rawDataFromFile.length()-16]; // We read the rest of the file (not including the IV)
            rawDataFromFile.read(cipherText);
            rawDataFromFile.close();

            //Set up the AES key & cipher object in CTR mode
            SecretKeySpec secretKeySpec = new SecretKeySpec(hexStringToByteArray(hexKey), "AES");
            Cipher decAESCTRcipher = Cipher.getInstance("AES/CTR/NoPadding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            decAESCTRcipher.init(Cipher.DECRYPT_MODE, secretKeySpec,ivSpec); // I changed this to DECRYPT_MODE

            //Decrypt the data
            byte[] plainText = decAESCTRcipher.doFinal(cipherText);

            //Write file to disk
            System.out.println("Opening file to write: "+outFile);
            FileOutputStream outToFile = new FileOutputStream(outFile);
            outToFile.write(plainText);
            outToFile.close();
            System.out.println(inFile+" encrypted as "+outFile);
        } catch (Exception e){
            System.out.println("doh "+e);
        }
    }

    private static void modifyCtrEnc() {
        try {
            // Open and read the input file
            RandomAccessFile rawDataFromFile = new RandomAccessFile(inFile, "r");
            byte[] iv = new byte[16];
            rawDataFromFile.read(iv);
            byte[] cipherText = new byte[(int) rawDataFromFile.length()-16];
            rawDataFromFile.read(cipherText);
            rawDataFromFile.close();

            // plainText and myPlainText
            byte[] plainText = "Pay jkh7030 1000 pounds\n".getBytes();
            byte[] myPlainText = "Pay cxs1068 9999 pounds\n".getBytes();

            // XOR plainText with cipherText to get encryptedCounter
            byte[] encryptedCounter = new byte[plainText.length];
            for (int i=0; i<plainText.length; i++) {
                encryptedCounter[i] = (byte) (plainText[i] ^ cipherText[i]);
            }

            // XOR myPlainText with encryptedCounter to get myCipherText
            byte[] myCipherText = new byte[myPlainText.length];
            for (int i=0; i<myPlainText.length; i++) {
                myCipherText[i] = (byte) (myPlainText[i] ^ encryptedCounter[i]);
            }

            //Write file to disk
            System.out.println("Opening file to write: "+outFile);
            FileOutputStream outToFile = new FileOutputStream(outFile);
            outToFile.write(iv);
            outToFile.write(myCipherText);
            outToFile.close();
            System.out.println(inFile+" encrypted as "+outFile);

        } catch (Exception e) {
            System.out.println("doh "+e);
        }
    }

    // Code from http://www.anyexample.com/programming/java/java%5Fsimple%5Fclass%5Fto%5Fcompute%5Fmd5%5Fhash.xml
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

    // Code from http://javaconversions.blogspot.co.uk
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

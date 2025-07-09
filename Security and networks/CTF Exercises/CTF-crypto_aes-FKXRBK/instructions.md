# AES Encryption and Decryption

  As a sensible security-minded individual, I've encrypted my sensitive flags using AES. 
  I split the data into two files encrypted with different AES modes as an experiment.
  However, now that I've done that, I can't access my data!

  In this folder I have the script that encrypted the files, as well as the encrypted files themselves. The script has the functions for encryption written, but not decryption.
  I know that I'll need to use the same key that I used for encryption, so I've noted that down here: `a98b0dcb31f5d431f73aee9c7442ada9`
  -J

## Introduction

For this challenge, your flag has been split into two strings, each of which have been encrypteed. You will need to decrypt their respective ciphertexts to get the two halves of the flag.  
As said above, there is a program written with the encryption function that made the ciphertexts. You will need to complete the program so that it can also decrypt files.  

In the provided zip you will find `AESCTR.enc` and `AESCCM.enc`. These are files that have been encrypted with AES in CTR mode and AES in CCM mode respectively. You will also find the Java file `AESEncTool.java` which is command line tool that can be used tool that can used to encrypt files.  

## Compiling and Running 

This tool uses the “Bouncy Castle” crypto libraries. These can be found in the `bcprov-jdk15on-151.jar` file in your home directory, and Java needs to be pointed to this. So you can compile the code using:
```javac -cp bcprov-jdk15on-151.jar EncTool.java```
and to run it using:
```java -cp bcprov-jdk15on-151.jar:. EncTool <mode> <key:128 bits in as hex> <inputFile> <outputFile>```
E.g. the ex2CTR.enc file was created using the command:
```java -cp bcprov-jdk15on-151.jar:. EncTool -encAESCTR a98b0dcb31f5d431f73aee9c7442ada9 plainText.txt ex2CTR.enc``` and the ex2CCM.enc file was created using the command:
```java -cp bcprov-jdk15on-151.jar:. EncTool -encAESCCM a98b0dcb31f5d431f73aee9c7442ada9 plainText.txt ex2CCM.enc```

## Tasks

1. Complete the method `decryptAESCTR()`, so that the program can decrypt messages encrypted with AES in CTR mode. Decrypt the `AESCTR.enc` file (with the key given above) for the first half of your flag.
2. Complete the method `decryptAESCCM()`, so that the program can decrypt messages encrypted with AES in CCM mode. Decrypt the `AESCCM.enc` file (with the key given above) for the second half of your flag. (You might also like to try editing some cipher texts and see that CCM mode detects the changes).
3. Combine the two flag fragments to recreate your flag and submit it online on the flag submission website.

## Getting help
Look at the lecture content, which should explain the theory behind the ciphers and the different modes. You can also view documentation online for the crypto libraries used. You can come to the lab sessions with questions and to get extra help with the exercise. You can also use the discussions on the Teams channel for questions or issues.
If you find an issue with the challenge or the flag website, please email one of the module leads or one of the TAs, you can find their contact details in the Canvas module.

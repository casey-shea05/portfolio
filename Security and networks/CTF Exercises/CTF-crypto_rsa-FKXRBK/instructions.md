# Public Key cryptography with RSA
After reading a bit more into some cryptography concepts, I've gotten really into RSA!
I have a file received from a friend thats been encrypted with AES. The file is called `RSA.enc`
The included AES key has been encrypted with my RSA public key, I'll need to use my private key to use it.

I started writing a script to automate everything, but atm only the encryption half is finished
Oh, just so I don't forget, the password for the keystore is `password`
-J

## Introduction

For this challenge, your flag has been encrypted in the file `RSA.enc`. You will need to decrypt this ciphertext to get your flag.
As said above, there is a program `RSAEncTool.java` written with the encryption function that made the ciphertexts. You will need to complete the program so that it can also decrypt files.  

As well as the ciphertext, you have been given `myKeyStore`, which is a Java key store file. This contains a key pair called `mykey` and is protected with the password `password`.

## Compiling and Running 

This tool uses the “Bouncy Castle” crypto libraries. These can be found in the `bcprov-jdk15on-151.jar` file in your home directory, and Java needs to be pointed to this. So you can compile the code using:
```javac -cp bcprov-jdk15on-151.jar RSAEncTool.java```

For RSA encryption, you must pass the name of the keyStore in your program invocation:
```java -cp bcprov-jdk15on-151.jar:. RSAEncTool -encRSA <keyStore> <keyName> <inputFile> <outputFile> ```
where `keyStore` is the file name of a key store and `keyName` is the name of the RSA key within the store. The tool will ask you for the password for the keystore which is “password”.
The `RSA.enc` file was created with the command:
```java  -cp bcprov-jdk15on-151.jar:. RSAEncTool -encRSA myKeyStore mykey plainText.txt RSA.enc```

## Tasks

1. Complete the method `decryptRSA()`, so that the program can decrypt messages encrypted with RSA mode. 
1. Decrypt the `RSA.enc` file (with the private key in the keystore). N.B. the RSA key used (and therefore the size of the encrypted AES key) is 2048 bits (= 256 bytes) long.
1. Submit your flag to the submission website.

## Getting help
Look at the lecture content, which should explain the theory behind the ciphers and the different modes. You can also view documentation online for the crypto libraries used. You can come to the lab sessions with questions and to get extra help with the exercise. You can also use the discussions on the Teams channel for questions or issues.
If you find an issue with the challenge or the flag website, please email one of the module leads or one of the TAs, you can find their contact details in the Canvas module.

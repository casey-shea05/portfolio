# AES-CTR Known plaintext attacks

  Fun fact: `ctr.enc` is a file encrypted with AES-CTR. Before encryption, this is what the file contained (with `\n` being the newline character):
  `Pay jkh7030 1000 pounds\n`
  
  I've heard about some pretty interesting things you can do with encrypted objects where you know the plaintext, so I want you to change it so that, when decrypted, `ctr.enc` reads:
  `Pay cxs1068 9999 pounds\n`,
  where you substitute the original target with your own username. 
  Can't give you the key though, that shouldn't be an issue right? :)
  Might also get you something nice in the mail, who knows!
  -J

## Introduction

As discussed in lectures, CTR mode encryption does not authenticate the encrypted data, therefore it can be altered by the attacker. The plaintext of the message encrypted in the file `ctr.enc` is `Pay jkh7030 1000 pounds\n`, and you do not have access to the encryption key.

To get the flag for this challenge, you will need to modify the ciphertext. We have generated individual ciphertexts for each of you that use your student username.
Change the ciphertext so that the decrypted message would read `Pay cxs1068 9999 pounds\n` (noting that `\n` is one character, the newline character). Bear in mind there are a few different methods in which you can modify the ciphertext for the correct result.

You will need to submit your modified `ctr.enc` to our server using the provided `knownplaintext_send.py` python script. We will then decrypt your ciphertext and check if it matches the expected output, returning your challenge flag if successful.

## Compiling and Running

We don't give you a program to edit for this challenge, only `ctr.enc` and the `knownplaintext_send.py` python script to send your input to the server.  

I strongly suggest that you make your own ciphertexts with `AESEncTool.java` from the AES challenge and experiment with them. If you haven't completed that challenge yet, I'd recommend you do that first!  

You may also try using hexeditors to look at the ciphertexts. One example hexeditor would be `ghex`, available on Linux systems, but any similar program on any operating system should work.  

### Submitting to the server

When you want to test your modified ciphertext, run
```python3 knownplaintext_send.py <YOUR TOKEN HERE> <YOUR MODIFIED CTR.ENC HERE>```
which will return a flag on a successful match to the target plaintext.
If you are connected to the campus eduroam network, you should be able to connect to the server fine. If you are off campus, you will need to use the University VPN.
You will also need python3 installed for this to work. Please don't edit the sending code, it will not help you get a flag :)

## Tasks
1. Modify `ctr.enc` to read `Pay cxs1068 9999 pounds\n` when decrypted with the correct key.
2. Submit the modified file to the server, getting the flag for the challenge.
3. Submit your flag on the flag submission website.

## Getting help
Look at the lecture content, which should explain the theory behind the ciphers and the different modes. You can also view documentation online for the crypto libraries used. You can come to the lab sessions with questions and to get extra help with the exercise. You can also use the discussions on the Teams channel for questions or issues.
If you find an issue with the challenge, the server decryption, or the flag website, please email one of the module leads or one of the TAs, you can find their contact details in the Canvas module.



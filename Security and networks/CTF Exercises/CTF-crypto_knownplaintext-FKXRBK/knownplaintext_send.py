import socket
import sys
from pathlib import Path

# DO NOT CHANGE BELOW!
HOST, PORT = "ec2-13-41-132-117.eu-west-2.compute.amazonaws.com", 3300
# simple arg handlding
if len(sys.argv)!= 3:
    print("Usage: python3 knownplaintext_send.py <token> <path_to_ciphertext.enc>")
    exit(1)

token = sys.argv[1].encode("utf-8")
ciphertext_path = sys.argv[2]

with open(ciphertext_path, "rb") as f:
    ciphertext = f.read()
    data = b''.join([str(len(ciphertext)).encode('utf-8'),ciphertext])
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        # Connect to server and send data
        sock.connect((HOST, PORT))
        sock.send(token)
        sock.send(data)

        # Receive data from the server and shut down
        received = sock.recv(1024).decode('utf-8')

    print(received)

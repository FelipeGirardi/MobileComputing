import json
import socket
from time import time

UDP_IP = "0.0.0.0"
UDP_BROADCAST_IP = "192.168.210.255"
UDP_PORT = 5004


def receive(sock):
    data, addr = sock.recvfrom(1024)
    packet = json.loads(data.decode("utf-8"))
    return packet, addr


def send(sock, packet, ip):
    data = json.dumps(packet).encode("utf-8")
    sock.sendto(data, (ip, UDP_PORT))


handled_requests = set()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.bind((UDP_IP, UDP_PORT))

while True:
    packet, addr = receive(sock)

    if packet["timestamp"] > 0:
        latency = (time() - packet["timestamp"]) * 1000
        print("{} from {} in {:.2f}ms".format(packet["message"], addr[0], latency))

    if packet["request_id"] not in handled_requests:
        handled_requests.add(packet["request_id"])
        packet["timestamp"] = time()
        send(sock, packet, UDP_BROADCAST_IP)

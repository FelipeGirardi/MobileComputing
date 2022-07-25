import json
import socket
from time import time

UDP_IP = "0.0.0.0"
UDP_BROADCAST_IP = "192.168.210.255"
UDP_PORT = 5004

node_ips = [
    "192.168.210.180",
    "192.168.210.196",
    "192.168.210.152",
    "192.168.210.197",
    "192.168.210.174",
]


def get_node_identifier():
    return socket.gethostname()[-1:]


def receive(sock):
    data, addr = sock.recvfrom(1024)
    packet = json.loads(data.decode("utf-8"))
    return packet, addr


def send(sock, packet, ip):
    data = json.dumps(packet).encode("utf-8")
    sock.sendto(data, (ip, UDP_PORT))


def send_on_route(sock, packet, route_index):
    receiver = packet["route"][route_index]
    receiver_ip = node_ips[
        int(receiver) - 1
    ]  # list starts at 0 but node_ids start at 1
    # print("sending reply to node {} ({})".format(receiver, receiver_ip))
    send(sock, packet, receiver_ip)


handled_requests = set()

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
sock.bind((UDP_IP, UDP_PORT))

node_id = get_node_identifier()

while True:
    packet, addr = receive(sock)
    print("{}: {} from {}".format(packet["type"], packet["route"], addr[0]))

    if packet["timestamp"] is None:  # if source node
        packet["timestamp"] = time()
        if packet["dest"] == node_id:
            print("source and destination node are the same")
        else:
            packet["route"] += node_id
            send(sock, packet, UDP_BROADCAST_IP)
            handled_requests.add(packet["request_id"])

    elif packet["type"] == "RREQ":
        if packet["request_id"] in handled_requests:
            continue

        packet["route"] += node_id
        if packet["dest"] == node_id:
            packet["type"] = "RREP"
            print("REQUEST reached destination, sending REPLY".format(node_id))
            send_on_route(sock, packet, len(packet["route"]) - 2)
        else:
            send(sock, packet, UDP_BROADCAST_IP)
        handled_requests.add(packet["request_id"])

    elif packet["type"] == "RREP":
        if packet["route"][0] == node_id:
            duration = (time() - packet["timestamp"]) * 1000
            print(
                "ROUTE: {} -> {} over {} discovered in {:.2f}ms".format(
                    node_id, packet["dest"], packet["route"], duration
                )
            )
        else:
            route_index = packet["route"].find(node_id)
            send_on_route(sock, packet, route_index - 1)

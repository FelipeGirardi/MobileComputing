#!/usr/bin/env python3

import socket
import sys

node_ips = [
    "192.168.210.180",
    "192.168.210.196",
    "192.168.210.152",
    "192.168.210.197",
    "192.168.210.174",
]

node_ip = node_ips[int(socket.gethostname()[-1:]) - 1]

if len(sys.argv) != 2:
    print("missing argument: file name")
    exit(1)

with open(sys.argv[1], "r+") as f:
    lines = f.readlines()

    for line in lines:
        print("[{}] {}".format(node_ip, line), end="")

    f.truncate(0)

#!/usr/bin/env bash

source $(dirname $0)/inc/common.sh

request_id=$(uuidgen)
packet='{"request_id": "'${request_id}'", "type": "RREQ", "route": "", "dest": "5", "timestamp": null}'

echo -n "$packet" | nc -u 129.69.210.178 5004 &
nc_pid=$!
sleep 2
kill $nc_pid
wait $nc_pid 2>/dev/null

for IP in $IPS
do
    ssh "${USER}@${IP}" "python3 helper.py output-dsr.txt"
done
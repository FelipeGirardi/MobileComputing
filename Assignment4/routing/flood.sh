#!/usr/bin/env bash

source $(dirname $0)/inc/common.sh

request_id=$(uuidgen)
packet='{"request_id": "'${request_id}'", "message": "very important information", "timestamp": 0}'

echo -n "$packet" | nc -u 129.69.210.152 5004 &
nc_pid=$!
sleep 2 
kill $nc_pid
wait $nc_pid 2>/dev/null

for IP in $IPS
do
    ssh "${USER}@${IP}" "python3 helper.py output-flood.txt"
done
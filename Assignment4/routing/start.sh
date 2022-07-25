#!/usr/bin/env bash

source $(dirname $0)/inc/common.sh

if [ "$#" -ne 1 ]; then
    echo "missing argument: program name (flood / dsr)"
    exit 1
fi

src="src/flood.py src/dsr.py src/helper.py"

prog="$1"

for IP in $IPS
do
    echo "$IP updating files"
    scp $src "${USER}@${IP}:${HOME_DIR}" > /dev/null
    echo "$IP run $prog.py"
    ssh "${USER}@${IP}" "pkill -u ${USER} python3; python3 -u ${prog}.py > output-${prog}.txt 2>&1 &"
done

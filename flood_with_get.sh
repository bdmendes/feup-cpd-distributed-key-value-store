#!/bin/bash
for((c=1; c<=$4;c++))
do
   ./run_client.sh "127.0.0.$1:$2" get $3
done

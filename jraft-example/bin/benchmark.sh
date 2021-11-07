#!/bin/bash

./bin/server_benchmark_start.sh localhost:8081,localhost:8082,localhost:8083 ./config/benchmark_server_8081.yaml server_out_8081
sleep 1
./bin/server_benchmark_start.sh localhost:8081,localhost:8082,localhost:8083 ./config/benchmark_server_8082.yaml server_out_8082
sleep 1
./bin/server_benchmark_start.sh localhost:8081,localhost:8082,localhost:8083 ./config/benchmark_server_8083.yaml server_out_8083
sleep 1


./bin/client_benchmark_start.sh localhost:8081,localhost:8082,localhost:8083 ./config/benchmark_client.yaml 8 1 9 20

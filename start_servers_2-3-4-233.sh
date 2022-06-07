#!/bin/sh

## Compiles and fires up a client with given command line arguments.
## Must be run from the repository root.
./gradlew build -q
cd build/classes/java/main || exit
java client.TestClient $1 %2 127.0.0.2 9002
java client.TestClient $1 %2 127.0.0.3 9002
java client.TestClient $1 %2 127.0.0.4 9002
java client.TestClient $1 %2 127.0.0.233 9002
cd ../../../..
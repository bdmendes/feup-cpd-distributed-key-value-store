#!/bin/sh

## Compiles and fires up a client with given command line arguments.
## Must be run from the repository root.
./gradlew build -q
cd build/classes/java/main || exit
java client.TestClient "$@"
cd ../../../..
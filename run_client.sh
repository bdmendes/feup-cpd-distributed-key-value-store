#!/bin/sh

## Fires up a client with given command line arguments.
## Must be run from the repository root.
cd build/ || exit
java client.TestClient "$@"
cd ../
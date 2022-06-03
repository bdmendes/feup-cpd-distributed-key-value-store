#!/bin/sh

## Compiles and fires up a server with given command line arguments.
## Must be run from the repository root.
cd build/|| exit
java server.Store "$@"
cd ../
#!/bin/sh

## Initializes the rmiregistry if it is not running and fires up a server with given command line arguments.
## Must be run from the repository root.
cd build/classes/java/main || exit
(pidof rmiregistry > /dev/null 2>&1) || (rmiregistry & sleep 1)
java server.Store "$@"
cd ../../../..
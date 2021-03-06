# Distributed key-value store

A distributed key-value store, using techniques such as consistent hashing and replication to provide a reliable, fast service.

>Check the project's report at *docs/* for information about the implementation details.

## Compilation instructions

### How to compile

In the root directory of the project, run one the following commands:

    ./compile.sh

or

    javac src/main/java/client/*.java src/main/java/communication/*.java src/main/java/message/*.java src/main/java/server/*.java src/main/java/utils/*.java src/main/java/message/messagereader/*.java src/main/java/server/state/*.java src/main/java/server/tasks/*.java  -d build/

This will compile the project and create the bytecode files in the *build/* directory.

### How to run

If loopback addresses are to be used, then for each address run:

    sudo ./add_lo_addr.sh <address>

There are two utilities to run the project:

One for creating a node:

    ./run_server.sh <multicast_address> <multicast_port> <node_ip> <node_port>

And another for running the client:

    ./run_client.sh <node_ap> <operation> [<operand>]

Where <node_ap> is the address of the node to connect to (<node_ip>:<node_port>) for put, get and delete operations
or the IP and RMI object name (<node_ip>:reg<node_ip>, eg. 127.0.0.1:reg127.0.1) for join and leave operations.

These utilities will run from the *build/* directory, so paths are relative to that.

## Group members

1. André Moreira (up201904721@edu.fe.up.pt)
2. Bruno Mendes (up201906166@edu.fe.up.pt)
3. Nuno Alves (up201908250@edu.fe.up.pt)

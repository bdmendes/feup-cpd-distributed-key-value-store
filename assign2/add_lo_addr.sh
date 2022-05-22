#!/bin/sh

## Adds an ip address to the loopback interface, to allow for multiple local servers testing scenarios.
ip addr add "$1" dev lo
ip route add 224.0.0.0/4 dev lo
ip l set lo multicast on
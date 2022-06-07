package utils;

import communication.RMIAddress;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RMIAddressTest {
    @Test
    void testIPAddress() {
        RMIAddress ip = new RMIAddress("127.0.0.1:server2");

        assertEquals("127.0.0.1", ip.getIp());
        assertEquals("server2", ip.getObjectName());
    }
}
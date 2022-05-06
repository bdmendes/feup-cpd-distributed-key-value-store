package utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IPAddressTest {
    @Test
    void testIPAddress() {
        IPAddress ip = new IPAddress("127.0.0.1:server2");

        assertEquals("127.0.0.1", ip.getIp());
        assertEquals("server2", ip.getObjectName());
    }
}
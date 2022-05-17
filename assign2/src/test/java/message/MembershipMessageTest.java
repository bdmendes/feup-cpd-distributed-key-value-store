package message;

import message.messagereader.MessageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Node;
import utils.MembershipLog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MembershipMessageTest {

    List<Node> nodes;
    private Map<String, Integer> membershipLog;

    @BeforeEach
    void generate() {
        nodes = new ArrayList<>();
        membershipLog = MembershipLog.generateMembershipLog();
        membershipLog.put("0", 0);
        membershipLog.put("1", 0);
        nodes.add(new Node("0", -1));
        nodes.add(new Node("1", -1));
    }


    @Test
    void encodeDecodeMessage() throws IOException {
        MembershipMessage membershipMessage = new MembershipMessage();
        nodes.forEach(membershipMessage::addNode);
        membershipLog.forEach((key, value) -> membershipMessage.getMembershipLog().put(key, value));
        byte[] data = membershipMessage.encode();

        MessageReader messageReader = new MessageReader();
        BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));

        while (!messageReader.isComplete()) {
            messageReader.read(in);
        }

        Message otherMembershipMessage = MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());

        assertEquals(membershipMessage, otherMembershipMessage);
    }

}
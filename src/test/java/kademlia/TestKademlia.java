package kademlia;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class TestKademlia {
    private final String path_routetable = "./routetable.csv";
    final int id_bit_length = 32;

    @After
    public void cleanup() {
        File file = new File(path_routetable);
        file.delete();
    }

    @Test
    public void node() {
        try {
            Map<BitSet, Node> all_nodes = new HashMap<BitSet, Node>();
            final int k = 20;
            Contact contact = new Contact(InetAddress.getByName("192.168.0.1"), 1235, id_bit_length);
            SocketNode socket = new SocketNode(all_nodes);
            Node bootstrap = new Node(socket, contact, k);
            all_nodes.put(bootstrap.me.id, bootstrap);
            contact = new Contact(InetAddress.getByName("192.168.0.2"), 1235, id_bit_length);
            Node node = new Node(socket, contact, k);
            all_nodes.put(node.me.id, node);

            assertTrue(node.ping(bootstrap.me));
            node.bootstrap(bootstrap.me);

            contact = new Contact(InetAddress.getByName("192.168.0.3"), 1235, id_bit_length);
            Node node1 = new Node(socket, contact, k);
            all_nodes.put(node1.me.id, node);
            node1.bootstrap(bootstrap.me);

            bootstrap.toCSV();
            node.toCSV();
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            fail("impossible happened" + e.toString());
        } catch (IOException e) {
            fail("impossible happened" + e.toString());
        }
    }
}

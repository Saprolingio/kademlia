package kademlia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVWriter;

import org.junit.After;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class TestKademlia
{
    private final String path_routetable = "./routetable.csv";
    @After
    public void cleanup() {

    }

    @Test
    public void node()
    {
        try {
            Map<BitSet, Node> all_nodes = new HashMap<BitSet, Node>();
            final int k = 20;
            Contact contact = new Contact(InetAddress.getByName("192.168.0.1"), 1234, k);
            SocketNode socket = new SocketNode(all_nodes);
            Node bootstrap = new Node(socket, contact, k);
            all_nodes.put(bootstrap.me.id, bootstrap);
            Node node = new Node(socket, contact, k);
            all_nodes.put(node.me.id, node);

            assertTrue(node.ping(bootstrap.me));
            node.bootstrap(bootstrap.me);

            CSVWriter csvw = new CSVWriter(new FileWriter(path_routetable));
            node.writeToCSV(csvw);
            csvw.close();
            List<String> lines_from_csv = Files.readAllLines(Paths.get(path_routetable));
            List<String> lines_expected_from_csv = Arrays.asList("id1", "id2");
            assertEquals(lines_from_csv, lines_expected_from_csv);
        } catch(UnsupportedEncodingException e) {
            fail("impossible happened" + e.toString());
        } catch(UnknownHostException e) {
            fail("impossible happened" + e.toString());
        } catch(IOException e) {
            fail("IO error: " + e.toString());
        }
    }
}

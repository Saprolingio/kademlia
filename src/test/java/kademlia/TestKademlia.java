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
import java.nio.file.Paths;
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
    final int id_bit_length = 32;

    @After
    public void cleanup() {
        File file = new File(path_routetable);
        file.delete();
    }

    @Test
    public void node()
    {
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

            //1 node
            CSVWriter csvw = Node.get_default_CSVWriter(path_routetable);
            node.writeToCSV(csvw);
            csvw.close();
            List<String> lines_from_csv = Files.readAllLines(Paths.get(path_routetable));
            List<String> lines_expected_from_csv = Arrays.asList("98989898,D3D3D3D3");
            assertEquals(lines_expected_from_csv, lines_from_csv);

            //2 nodes
            contact = new Contact(InetAddress.getByName("192.168.0.3"), 1235, id_bit_length);
            node = new Node(socket, contact, k);
            all_nodes.put(node.me.id, node);
            node.bootstrap(bootstrap.me);

            csvw = Node.get_default_CSVWriter(path_routetable);
            for(Map.Entry<BitSet, Node> entry : all_nodes.entrySet()) {
                node = entry.getValue();
                node.writeToCSV(csvw);
            }
            csvw.close();
            lines_from_csv = Files.readAllLines(Paths.get(path_routetable));
            lines_expected_from_csv = Arrays.asList(
                "D3D3D3D3,98989898",
                "D3D3D3D3,88888888",
                "98989898,D3D3D3D3",
                "98989898,88888888",
                "88888888,98989898",
                "88888888,D3D3D3D3");
            
            assertEquals(lines_expected_from_csv, lines_from_csv);

        } catch(UnsupportedEncodingException e) {
            fail("impossible happened" + e.toString());
        } catch(UnknownHostException e) {
            fail("impossible happened" + e.toString());
        } catch(IOException e) {
            fail("IO error: " + e.toString());
        }
    }

    @Test
    public void coordinator() {
        String [] params = {"-m 5 -n 16"};
        Coordinator.main(params);
    }
}

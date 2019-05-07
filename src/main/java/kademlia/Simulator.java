package kademlia;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import com.opencsv.CSVWriter;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import static net.andreinc.mockneat.unit.types.Ints.ints;
import static net.andreinc.mockneat.unit.networking.IPv6s.ipv6s;

@Command(mixinStandardHelpOptions = true, version = "v1.0.0", header = "Kademlia Simulator.", description = {
    "This tool will produce a pari of csv files:",
    "\t* routing_table*.csv containing tuple <SOURCE, TARGET, JOIN_NODE, N_RECIVED_FINDNODE> compatible with cytoscape;",
    "\t* the other it's a routing table of the first kademlia node.",
    })
public class Simulator {
    @Spec public CommandSpec spec;
    @ArgGroup(exclusive = false, multiplicity = "1")
    public Real_params params;

    static class Real_params {
        @Option(names = { "-m", "--id_bit_length" }, paramLabel = "bit_len", required = true,  description = "Number of bits of the identifiers of the Kademlia network (default: ${DEFAULT-VALUE}).")
        public int bit_len;
        
        @Option(names = { "-n", "--nodes" }, paramLabel = "n_nodes", required = true, description = "Number of nodes tha will join the network (default: ${DEFAULT-VALUE}).")
        public int n_nodes;
    };

    @Option(names = { "-o", "--output" }, paramLabel = "filename", required = false, description = "The csv filename where write the output, otherwise will be self generated.")
    public String output = "";

    @Option(names = { "-k"}, paramLabel = "k", defaultValue = "20", required = true, description = "The dimension of the kbucket (default: ${DEFAULT-VALUE}).")
    public int k;

    @Option(names = { "-l", "--lookups"}, paramLabel = "n_lookups", defaultValue = "0", required = true, description = "The number of random lookups (default: ${DEFAULT-VALUE}).")
    public int lookups = 0;

    @Option(names = { "-a", "--alpha"}, paramLabel = "n_lookups", defaultValue = "3", required = true, description = "The is the number of parallel lookups. It is also the number of elements taken from a routing table during a findNode (default: ${DEFAULT-VALUE}).")
    public int alpha;

    @Option(names = { "-r", "--recursive"}, defaultValue = "false", required = true, description = "Select between a fullLookup function and a lighter recursivefindNode. (default: ${DEFAULT-VALUE}).")
    public boolean recursive;

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        CommandLine commandLine = new CommandLine(simulator);
        commandLine.setUsageHelpWidth(160);
        try {
            commandLine.parseArgs(args);
            if (commandLine.isUsageHelpRequested()) {
                commandLine.usage(System.out);
                return;
            }

            if (simulator.params.bit_len <= 0 || simulator.params.n_nodes <= 0) 
                throw new ParameterException(simulator.spec.commandLine(), "Arguments must be greater than 0");
            if (Math.pow(2, simulator.params.bit_len) < simulator.params.n_nodes) 
                throw new ParameterException(simulator.spec.commandLine(), "bit len insufficient for this number of nodes [" + simulator.params.bit_len +
                    "] ->" + Math.pow(2, simulator.params.bit_len) + "<" + simulator.params.n_nodes + "<-");
            
            if(simulator.output.equals("")) {
                StringBuilder out = new StringBuilder();
                //autogenerate output
                out.append("routing_table");
                out.append("-m");
                out.append(simulator.params.bit_len);
                out.append("-n");
                out.append(simulator.params.n_nodes);
                out.append("-k");
                out.append(simulator.k);
                out.append("-l");
                out.append(simulator.lookups);
                out.append("-a");
                out.append(simulator.alpha);
                if(simulator.recursive)
                    out.append("-r");
                out.append(".csv");
                simulator.output = out.toString();
            }
            
            simulator.start();

        } catch (MissingParameterException ex) {
            System.err.println("Error parsing commandline: " + ex.getMessage());
        } catch (ParameterException ex) {
            System.err.println("Somthing wrong in parameters: " + ex.getMessage());
        }
    }

    Map<BitSet, Node> all_nodes = new HashMap<BitSet, Node>();
    ArrayList<Node> joined_nodes = new ArrayList<Node>();
    SocketNode socket = new SocketNode(all_nodes);
    
    /**
     * Generate a random id
     * @return return a random bitset representing the id
     * @throws UnknownHostException should never be thrown (caused by random generated ip)
     * @throws UnsupportedEncodingException should never be thrown (caused by random generated ip)
     */
    private BitSet randomBitset() throws UnknownHostException, UnsupportedEncodingException {
        return Contact.hash(InetAddress.getByName(ipv6s().get()), ints().range(0, 65535).get(), this.params.bit_len);
    }

    /**
     * Generate create a node with random id, and putting it into the joined_node lis
     * @return the new generate node
     * @throws UnknownHostException should never be thrown (caused by random generated ip)
     * @throws UnsupportedEncodingException should never be thrown (caused by hash)
     */
    private Node nodeJoining() throws UnknownHostException, UnsupportedEncodingException{
        Contact c;
        do {
            c = new Contact(this.params.bit_len);    //generate a random contact not already in use
        } while(this.all_nodes.containsKey(c.id));
        Node n = new Node(this.socket, c, this.k, this.alpha);
        this.all_nodes.put(n.me.id, n);
        this.joined_nodes.add(n);
        return n;
    }
    /**
     * Take a random node from already joined list
     * @return
     */
    private Node randomBootstrap() {
        int rand_pos = ints().range(0, joined_nodes.size()-1).get();
        Node rand_node = this.joined_nodes.get(rand_pos);
        return rand_node;
    }

    private static interface Operation {
        void exec(Node node, Node bootstrap, BitSet id);
    };

    public void start() {
        try {
            Node bootstrap = this.nodeJoining();
            Node first = bootstrap;
            Node node = bootstrap;

            Operation pre_lookup, post_lookup;
            // setup for recursive/ lookup
            if(this.recursive) {
                pre_lookup = (n, b, i) -> {
                    ShortList traversed = new ShortList(this.k, b.me, i);
                    traversed.add(n.me);
                    n.recursiveFindNode(b.me, i, traversed);
                };
                post_lookup = pre_lookup;
            } else {
                pre_lookup = (n, b, i) -> {
                    n.bootstrap(b.me);
                };
                post_lookup = (n, b, i) -> {
                    n.Lookup(i);
                };
            }

            for(int n_nodes = this.params.n_nodes - 1; n_nodes > 0; n_nodes--) {
                node = this.nodeJoining();
                pre_lookup.exec(node, bootstrap, node.me.id);
                if(this.lookups > 0) {
                    // generate a random id and fitting it to a bucket list
                    for(int bucket_index = 0; bucket_index < this.params.bit_len; bucket_index++)
                        for(int n_lookups = this.lookups; n_lookups > 0; n_lookups--) {
                            BitSet id = this.randomBitset();
                            for(int bit_to_set = bucket_index; bit_to_set < this.params.bit_len; bit_to_set++) {
                                if(node.me.id.get(bit_to_set))
                                    id.set(bit_to_set);
                                else
                                    id.clear(bit_to_set);
                            }
                            post_lookup.exec(node, bootstrap, id);
                        }
                }
                System.out.print("\r node progress: " + node.node_number + "/" + this.params.n_nodes);  // progress status
                bootstrap = this.randomBootstrap();
            }
            first.toCSV();
            node.toCSV();

            CSVWriter csvw = Node.getDefaultCSVWriter(this.output);
            for(Map.Entry<BitSet, Node> entry : all_nodes.entrySet()) {
                node = entry.getValue();
                node.writeToCSV(csvw);
            }
            csvw.close();
        } catch (UnknownHostException e) {
            System.err.println("Should not appen: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("Should not appen: " + e.getMessage());
        } catch(IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }
}

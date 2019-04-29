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
    
    private BitSet randomBitset() throws UnknownHostException, UnsupportedEncodingException {
        return Contact.hash(InetAddress.getByName(ipv6s().get()), ints().range(0, 65535).get(), this.k);
    }

    private Node nodeJoining() throws UnknownHostException, UnsupportedEncodingException{
        Contact c;
        do {
            c = new Contact(this.k);
        } while(this.all_nodes.containsKey(c.id));
        Node n = new Node(this.socket, c, this.k, this.alpha);
        this.all_nodes.put(n.me.id, n);
        this.joined_nodes.add(n);
        return n;
    }

    private Node randomBootstrap(Node node) {
        int rand_pos = ints().range(0, joined_nodes.size()-1).get();
        Node rand_node = this.joined_nodes.get(rand_pos);
        if(rand_node.me.equals(node.me)) // ensure not same node
            rand_node = this.joined_nodes.get((rand_pos + 1) % joined_nodes.size());
        return rand_node;
    }

    public void start() {
        try {
            Node bootstrap = this.nodeJoining();
            Node first = bootstrap;
            Node node;
            for(int n_nodes = this.params.n_nodes - 1; n_nodes > 0; n_nodes--) {
                node = this.nodeJoining();
                bootstrap = randomBootstrap(node);
                if(!this.recursive)
                    node.bootstrap(bootstrap.me);
                if(this.lookups > 0) {
                    for(int bucket_index = 0; bucket_index < this.k; bucket_index++)
                        for(int n_lookups = this.lookups; n_lookups > 0; n_lookups--) {
                            BitSet id = this.randomBitset();
                            for(int bit_to_set = bucket_index; bit_to_set < this.k; bit_to_set++) {
                                if(node.me.id.get(bit_to_set))
                                    id.set(bit_to_set);
                                else
                                    id.clear(bit_to_set);
                            }
                            if(this.recursive) {
                                ShortList traversed = new ShortList(this.k, bootstrap.me);
                                traversed.add(node.me);
                                node.recursiveFindNode(bootstrap.me, id, traversed);
                            } else 
                                node.Lookup(id);
                        }
                }
            }
            first.toCSV();

            CSVWriter csvw = Node.get_default_CSVWriter(this.output);
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

package kademlia;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
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

@Command(mixinStandardHelpOptions = true, version = "v1.0.0", header = "Kademlia Simulator")
public class Simulator {
    private final static String program_name = "Kademlia Simulation";
    
    @Spec public CommandSpec spec;

    @ArgGroup(exclusive = false, multiplicity = "1")
    public Real_params params;

    static class Real_params {
        @Option(names = { "-m", "--id_bit_length" }, paramLabel = "bit_len", required = true,  description = "Number of bits of the identifiers of the Kademlia network (default: ${DEFAULT-VALUE}).")
        public int bit_len;
        
        @Option(names = { "-n", "--nodes" }, paramLabel = "n_nodes", required = true, description = "Number of nodes tha will join the network (default: ${DEFAULT-VALUE}).")
        public int n_nodes;
    };

    @Option(names = { "-o", "--output" }, paramLabel = "filename", defaultValue = "o.csv", required = true, description = "The csv filename where write the output (default: ${DEFAULT-VALUE}).")
    public String output;

    @Option(names = { "-k"}, paramLabel = "k", defaultValue = "20", required = true, description = "The dimension of the kbucket (default: ${DEFAULT-VALUE}).")
    public int k;

    public static void main(String[] args) {
        Simulator simulator = new Simulator();
        CommandLine commandLine = new CommandLine(simulator);
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
            
                simulator.start();

        } catch (MissingParameterException ex) {
            System.err.println("Error parsing commandline: " + ex.getMessage());
        } catch (ParameterException ex) {
            System.err.println("Somthing wrong in parameters: " + ex.getMessage());
        }
    }

    Map<BitSet, Node> all_nodes = new HashMap<BitSet, Node>();
    SocketNode socket = new SocketNode(all_nodes);    

    private Node nodeJoining() throws UnknownHostException, UnsupportedEncodingException{
        Node n = new Node(this.socket, new Contact(this.params.bit_len), this.k);
        this.all_nodes.put(n.me.id, n);
        return n;
    }

    public void start() {
        try {
            Node bootsrap = this.nodeJoining();
            Node node;
            for(int n_nodes = this.params.n_nodes - 1; n_nodes > 0; n_nodes--) {
                node = this.nodeJoining();
                node.bootstrap(bootsrap.me);
            }

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

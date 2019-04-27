package kademlia;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Coordinator {
    private final static String program_name = "Kademlia Simulation";
    public static void main(String[] args) {
        Options options = new Options();
        CommandLineParser parser = new DefaultParser();
        options.addOption(Option.builder("m")
            .longOpt("id_bit_length")
            .argName("m" ) 
            .hasArg()
            //.numberOfArgs(1)
            .type(Integer.class)
            .desc("Number of bits of the identifiers of the Kademlia network")
            .build());

        options.addOption(Option.builder("n")
            .longOpt("nodes")
            .argName("n" ) 
            .hasArg()
            //.numberOfArgs(1)
            .type(Integer.class)
            .desc("Number of nodes tha will join the network")
            .build());

        options.addOption("help", false, "print this helper");
        
        try {
            CommandLine cmd = parser.parse(options, args);
            if(cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(program_name, options);
                return;
            }

            if(!cmd.hasOption("m") && !cmd.hasOption("n"))
                throw new ParseException("Missing required options: m, n");
            /*
            final int bit_len = cmd.getOptionValue("m");
            final int n_nodes = cmd.getOptionValue("n");
            */

        } catch (ParseException e) {
            System.err.println("Error while parsing command line: " + e.getMessage());
        }
    }
}

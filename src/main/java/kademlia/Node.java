package kademlia;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import com.opencsv.CSVWriter;

/**
 * Node of Kademlia network
 */
class Node {

    // visualization utility
    private static long node_counter = 1; //!< used to check the chronological order of creation
    public final long node_number; //!< this node join order
    private long received_find_node; //!< how many find_node this Node has received

    public final int alpha; //!< kademlia parameter (usually 3), used with Shortlist
    private final int k; //!< length of buckets
    public final Contact me; //!< Node contact information (and id)
    private SocketNode socket;
    private Klist[] routing_table;

    /**
     * The only Node constructor.
     * @param socket used to send message to other nodes.
     * @param me Node contact information (and id)
     * @param k  //!< length of buckets
     * @param alpha //!< kademlia parameter
     */
    public Node(SocketNode socket, Contact me, int k,  int alpha) {
        this.alpha = alpha;
        this.k = k;
        this.me = me;
        this.socket = socket;
        this.routing_table = new Klist[me.id_bit_length];
        this.node_number = node_counter++;
        this.received_find_node = 0;
    }

    /**
     * Bootstrap procedure. After creation must be called.
     * This insert the first node and execute the first lookup using me.id as target
     * @param bootstrap the first node inserted int he routing table
     */
    public void bootstrap(Contact bootstrap){
        ShortList list = new ShortList(this.k, this.me, this.me.id);
        list.add(bootstrap);
        this.updateKlist(list);
        this.Lookup(this.me.id);
    }

    /**
     * Simulate receiving a message
     * @param msg the message received
     * @return the response message
     */
    public Message receive(Message msg) {
        switch(msg.type) {
            case PING:
                //refresh the sender contact in the routing table
                this.getKbucket(msg.sender).refresh(msg.sender);
                return new Message.Response(msg);
            case FIND:
                Message.FindRequest fr = (Message.FindRequest) msg;
                this.received_find_node++;
                this.updateKlist(fr.traversed_nodes);
                return new Message.FindResponse(this.findNode(fr.id), msg);
            default:
                return null; //should never appen
        }
    }

    /**
     * PING primitive.
     * simply send a message and check if pong is recived.
     * @param who to who send the ping
     * @return if pong is received (true) or not (false)
     */
    public boolean ping(Contact who) {
        Message msg = new Message(Message.kind.PING, this.me, who);
        msg = this.socket.sendAndReceive(msg);
        return msg != null;
    }

    /**
     * Add all element of the Shortlist list, to the routing table.
     * using he utility addContact prevent duplicates.
     * @param list the elements to add
     */
    private void updateKlist(ShortList list) {
        if(list == null)
            return;

        for (Element el : list)
            this.addContact(el.contact);
    }

    /**
     * Used for debug (not used in the code).
     * It provide a bitString for a BitSet
     * @param b the bitset to stringify
     * @return the stringified bitset
     */
    @SuppressWarnings("unused") 
    private static String toByteString(BitSet b) {
        StringBuilder res = new StringBuilder();
        for(int i = b.size(); i > 0; i--)
            if(b.get(i))
                res.append("1");
            else
                res.append("0");
        return res.toString();
    }

    /**
     * Search for most appropriate bucket to contain the contact provided.
     * @param contact the contact to chose the klist
     * @return the appropriate klist to contain contact, null if contact is the same as this Node
     */
    private Klist getKbucket(Contact contact) {
        BitSet app = (BitSet) contact.id.clone();
        app.xor(this.me.id);
        final int pos = app.previousSetBit(app.length()-1);
        if(pos == -1)
            return null;
        Klist klist = this.routing_table[pos];
        if(klist == null) {
            klist = new Klist(this.k);
            this.routing_table[pos] = klist;
        }
        return klist;
    }

    /**
     * Function that handle the insertion of the contact.
     * * the contact is not insert if is equal to the contact of this node;
     * * the contact is refreshed if present and reachable;
     * * the contact is added if there is room in the appropriate klist and is not present;
     * * the contact is not inserted at all if the klist is full and the head of the klist is reachable.
     * @param contact the contact that wanna be add
     */
    private void addContact(Contact contact) {
        if(contact.equals(this.me))
            return;
        Klist klist = this.getKbucket(contact);
        if(klist == null) //should be prevented by previous equals, no bucket for same node
            return;

        Contact res = klist.addContact(contact); //check if there is a scapegoat
        if(res != null) { //gotcha!
            //if added is equal to scapegoat, refresh it, also if not but is reachable
            if(res.equals(contact) || this.ping(res))
                klist.addContact(res); // refresh this contact
            else
                klist.addContact(contact);  // old contact timed out, replace it with new contact
        } //else is added
    }

    /**
     * A primitive operation of kademlia. Query a Node for closest Contact to id.
     * The procedure consist into searching for the best fit klist, take alpha elements from it
     * and then move to near bucket and take alpha element from tham until fulling
     * the list with size k.
     * @param id the searched id
     * @return a list containing at most k elements, taken alpha from each bucket.
     */
    private ShortList findNode(BitSet id) {
        ShortList res = new ShortList(this.k, this.me, id);
        int pos;
        if(this.me.id.equals(id)) {
            res.add(this.me); //if you are searching for me, here I am!
            pos = this.routing_table.length / 2;    //start from half to take closest to me
        } else {
            BitSet app = (BitSet) me.id.clone();
            app.xor(id);
            pos = app.previousSetBit(app.length()-1); //searching for the fittest klist position
        }

        Klist list = null;
        //iterate over klist backward to add element to shortlist until k elment are added
        for(int j = 0; res.size() < this.k && j < this.routing_table.length; j++) {
            list = this.routing_table[pos];
            pos = (pos == 0 ? this.routing_table.length : pos) - 1;
            if(list == null)
                continue;
            for(Contact c: list) {
                res.add(c);
                if(res.size() == this.k)
                    return res;
            }
        }
        return res;
    }

    /**
     * Searching for the node identified by id
     * alpha node are contacted for each step. This function is conceptually
     * recursive (repeat recursion until no best node then previous closes node is found).
     * but for simplicity is written iterative
     * @param id the id to be searched
     */
    public void Lookup(BitSet id) {
        ShortList short_list = findNode(id);
        short_list.sort();
        ShortList traversed = new ShortList(this.k, this.me, id);
        traversed.add(this.me);
        ArrayList<Element> list = short_list.getAlpha(this.alpha);
        if(list.size() != 0) {
            Contact nearest;
            Contact new_nearest = list.get(0).contact;
            do {
                nearest = new_nearest;
                /*
                    is wrote in functional style to switch easily to real parallel search
                    but in this simulation synchronization problem may arise
                    (but not in the real case in witch the nodes are separate distributed processes)
                */
                ShortList requested = list.stream().map(el -> {
                    Message.FindRequest msg = new Message.FindRequest(id, traversed, this.me, el.contact);
                    Message.FindResponse res = (Message.FindResponse) this.socket.sendAndReceive(msg);
                    if(res == null)
                        return null;
                    else{
                        el.setContacted();
                        return res.shortlist;
                    }
                }).reduce((sl1, sl2) -> {
                    sl1.addAll(sl2);
                    return sl1;
                }).get();

                this.updateKlist(requested);
                short_list.merge(requested);
                //add contacted node to traversed
                for (Element el : list) {
                    if(el.getContacted())
                        traversed.add(el);
                }
                list = short_list.getAlpha(this.alpha);
                nearest = short_list.get(0).contact;
            } while(nearest.equals(new_nearest) && list.size() != 0);
            /*
                The termination condition is when no more node to be contacted are present,
                or when the closest node remain the same (no improvement).
            */

            //contact all te remaining node in the list anyway
            for(; list.size() != 0; list = short_list.getAlpha(this.alpha)) {
                for (Element el : list) {
                    Message.FindRequest msg = new Message.FindRequest(id, traversed, this.me, el.contact);
                    Message.FindResponse res = (Message.FindResponse) this.socket.sendAndReceive(msg);
                    if(res != null)
                        el.setContacted();
                        this.updateKlist(res.shortlist);
                }
            }
        }
    }

    /**
     * A simplier recusive version of the lookup
     * It perform worst due to its semplicity
     * @param nearest the nearest known node
     *  (provide this.me for the first call)
     * @param id    the id to be searched
     * @param traversed the list of node traversed
     *  (provide a new Shortlist containing this.me for the first)
     */
    public void recursiveFindNode(Contact nearest, final BitSet id, ShortList traversed) {
        Message.FindRequest msg = new Message.FindRequest(id, traversed, this.me, nearest);
        Message.FindResponse res = (Message.FindResponse) this.socket.sendAndReceive(msg);
        if(res == null || res.shortlist.size() == 0)
            return;

        res.shortlist.sort();
        this.updateKlist(res.shortlist);
        Contact newNearest = res.shortlist.get(0).contact;
        if(newNearest.distance(this.me) >= nearest.distance(this.me))
            return;

        traversed.add(nearest);
        recursiveFindNode(newNearest, id, traversed);
    }

    private static String[] headers = {"SOURCE", "TARGET", "JOIN_NUMBER", "RECEIVED_FINDNODE"}; //!< default header for csw cytoscape compatible
    /**
     * Get a CSVWrite setted as default
     * @param path path to a file
     * @return the writer to be used in writeToCSV
     * @throws IOException if there is problem creating or accessing the file (path)
     */
    public static CSVWriter getDefaultCSVWriter(String path) throws IOException
    {
        CSVWriter writer = new CSVWriter(new FileWriter(path),  ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        writer.writeNext(headers, true);
        return writer;
    }

    /**
     * Utility function append a n*c char String to buf.
     * Used to fill an cell of csv with the same char.
     * @param buf buffer to which add the result.
     * @param n how many time repeat c
     * @param c the char to repeat
     */
    private static void printN(List<String> buf, int n, char c) {
        StringBuilder strbuf = new StringBuilder();
        for(int i = 0; i < n; i++)
            strbuf.append(c);
        buf.add(strbuf.toString());
    }

    /**
     * Utility function similar to printN but pre-pending a string.
     * Used to fill an cell of csv with the same char, but pre-pending a String.
     * @param buf buffer to which add the result.
     * @param n how many time repeat c
     * @param c the char to repeat
     * @param prepends string to prepend
     */
    private static void printN(List<String> buf, int n, char c, String prepend) {
        StringBuilder strbuf = new StringBuilder();
        strbuf.append(prepend);
        for(int i = 0; i < n; i++)
            strbuf.append(c);
        buf.add(strbuf.toString());
    }

    /**
     * Utility function to write a to writer row and clear the row buffer.
     * @param writer the csv writer
     * @param list the list containing the columns (for this row) to write.
     */
    private static void write(CSVWriter writer, List<String> list) {
        String[] tmp = list.toArray(new String[0]);
        writer.writeNext(tmp);
        list.clear();
    }

    /**
     * Used to check the content of the routing table.
     * write the representation of the routing table to a csv file, to
     * be easily read using Excel like programs.
     * @return return the file name generated using the id of this node
     * @throws IOException if the file cannot be opened or created
     */
    public String toCSV() throws IOException {
        String hexId = this.me.idString() + "_" + this.node_number +".csv";
        CSVWriter writer = new CSVWriter(new FileWriter(hexId));
        String id = this.me.idByteString();
        List<String> buf = new ArrayList<String>();
        buf.add(this.me.idString());
        buf.add(" ---> ");
        buf.add(id);
        write(writer, buf);
        for(int i = 0; i < this.me.id_bit_length; i++) {
            if(this.routing_table[i] == null)
                printN(buf, id.length(), 'N');
            else
                printN(buf, i + 1, '*', id.substring(0, this.me.id_bit_length - i - 1));
        }
        write(writer, buf);

        boolean loop = true;

        ArrayList<ArrayList<Contact>> contacts = new ArrayList<>();
        for (Klist kl : this.routing_table) {
            if(kl == null)
                contacts.add(new ArrayList<>());
            else
                contacts.add(new ArrayList<Contact>(kl));
        }
        for(int i =0; loop; i++) {
            loop = false;
            for (ArrayList<Contact> kl : contacts) {
                if(kl.size() > i) {
                    loop = true;
                    buf.add(kl.get(i).idByteString());
                } else {
                    buf.add("");
                }
            }
            write(writer, buf);
        }
        writer.close();
        return hexId;
    }


    /**
     * Write this node in a format compatible to Cytoscape.
     * A writer should be provided because multiple Nodes should be wrote to the same file
     * @param csvw he csvwriter
     */
    public void writeToCSV(CSVWriter csvw) {
        String [] row = new String[headers.length];
        for (Klist klist : this.routing_table) {
            if(klist != null)
                for(Contact c: klist){
                    row[0] = this.me.idString();
                    row[1] = c.idString();
                    row[2] = Long.toString(this.node_number);
                    row[3] = Long.toString(this.received_find_node);
                    csvw.writeNext(row);
                }
        }
    }
};
package kademlia;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

import com.opencsv.CSVWriter;

class Node {
    public final static int alpha = 3;

    // visualizzation addittion
    private static long node_counter = 0;
    public final long node_number; 
    private long received_findnode; 

    private final int k;
    public final Contact me;
    private SocketNode socket;
    private Klist[] routing_table;
    //private Map<BitSet, Object> stored_objects;

    public Node(SocketNode socket, Contact me, int k) {
        this.k = k;
        this.me = me;
        this.socket = socket;
        this.routing_table = new Klist[this.me.id_bit_length];
        this.node_number = node_counter++;
        this.received_findnode = 0;
    }

    public void bootstrap(Contact bootstrap){
        ShortList list = new ShortList(this.k, this.me);
        list.add(bootstrap);
        this.updateKlist(list);
        this.Lookup(this.me.id);
    }

    public Message receive(Message msg) {
        switch(msg.type) {
            case PING:
                this.addContact(msg.sender);
                return new Message.Response(msg);
            case FIND:
                Message.FindRequest fr = (Message.FindRequest) msg;
                this.received_findnode++;
                this.updateKlist(fr.traversed_nodes);
                return new Message.FindResponse(this.findNode(fr.id), msg);
            case STORE:
                //TODO
                return null;
            default:
                return null; //should never appen
        }
    }

    public boolean ping(Contact who) {
        Message msg = new Message(Message.kind.PING, this.me, who);
        msg = this.socket.sendAndReceive(msg);
        return msg != null;
    }

    private void updateKlist(ShortList list) {
        if(list == null)
            return;

        for (Element el : list)
            if(!el.contact.equals(this.me))
                this.addContact(el.contact);
    }

    private void addContact(Contact contact) {
        BitSet app = (BitSet) contact.id.clone();
        app.xor(this.me.id);
        final int pos = app.length() / 8;
        Klist klist = this.routing_table[pos];
        if(klist == null) {
            klist = new Klist(this.k);
            this.routing_table[pos] =  klist;
        }

        Contact res = klist.addContact(contact);
        if(res != contact) {
            Message msg = new Message(Message.kind.PING, this.me, res); //ping if alive
            msg = this.socket.sendAndReceive(msg);
            if(msg == null) //timed out
                klist.replace(contact);
            else
                klist.refresh(res);
        }
    }

    private ShortList findNode(BitSet id) {
        BitSet app = (BitSet) me.id.clone();
        app.xor(id);
        final int pos = app.length() / 8;
        Klist list = null;
        ShortList res = new ShortList(this.k, this.me);
        for(int i = pos; res.size() < this.k && i < this.routing_table.length; i++) {
            list = this.routing_table[i];
            if(list == null)
                continue;
            for(Contact c: list)
                res.add(c);
        }
        for(int i = pos - 1; res.size() < this.k && i >= 0; i--) {
            list = this.routing_table[i];
            if(list == null)
                continue;
            for(Contact c: list)
                res.add(c);
        }

        return res;
    }

    public void Lookup(BitSet id) {
        ShortList short_list = findNode(id);
        ShortList traversed = new ShortList(this.k, this.me);
        traversed.add(this.me);
        ArrayList<Element> list = short_list.getAlpha(Node.alpha);
        short_list.sort();
        while(list.size() != 0) {
            ShortList requested = list.stream().map(el -> {
                Message.FindRequest msg = new Message.FindRequest(id, traversed, this.me, el.contact);
                Message.FindResponse res = (Message.FindResponse) this.socket.sendAndReceive(msg);
                if(res == null)
                    return null;
                else{
                    el.set_contacted();
                    return res.shortlist;
                }
            }).reduce((sl1, sl2) -> {
                sl1.addAll(sl2);
                return sl1;
            }).get();

            this.updateKlist(requested);
            short_list.merge(requested);
            traversed.addAll(list); // FIXME not all nodes are shurely contacted
            list = short_list.getAlpha(Node.alpha);
            System.out.println(traversed.size() + " --- " + list.size());
        }
    }

    public void store() {}   // TODO sss

    private static String[] headers = {"SOURCE", "TARGET", "JOIN_NUMBER", "RECEIVED_FINDNODE"};
    public static CSVWriter get_default_CSVWriter(String path) throws IOException
    {
        CSVWriter writer = new CSVWriter(new FileWriter(path),  ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        writer.writeNext(headers, true);
        return writer;
    }

    public void writeToCSV(CSVWriter csvw) {
        String [] row = new String[headers.length];
        for (Klist klist : this.routing_table) {
            if(klist != null)
                for(Contact c: klist){
                    row[0] = this.me.idString();
                    row[1] = c.idString();
                    row[2] = Long.toString(this.node_number);
                    row[3] = Long.toString(this.received_findnode);
                    csvw.writeNext(row);
                }
        }
    }
};
package kademlia;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

class Node {
    private ArrayList<Klist> routing_table;
    private final int k;
    private SocketNode socket;
    private Map<BitSet, Object> stored_objects;
    public final Contact me;

    public Node(SocketNode socket, Contact me, int k) {
        this.me = me;
        this.socket = socket;
        this.k = k;
        this.Lookup();
    }

    public Message receive(Message msg) {
        switch(msg.type) {
            case PING:
                return new Message.Response(msg);
            case FIND:
                Message.FindRequest fr = (Message.FindRequest) msg;
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
        for (Element el : list) {
            BitSet app = (BitSet) el.contact.id.clone();
            app.xor(this.me.id);
            final int pos = app.length() / 8;
            Klist klist = this.routing_table.get(pos);
            if(klist == null) {
                klist = new Klist(this.k);
                this.routing_table.add(pos, klist);
            }
            Contact res = klist.addContact(el.contact);
            if(res != el.contact) {
                Message msg = new Message(Message.kind.PING, this.me, res); //ping if alive
                msg = this.socket.sendAndRecive(msg);
                if(msg == null) //timed out
                    klist.replace(el.contact);
                else
                    klist.refresh(res);
            }
        }
    }

    private ShortList findNode(BitSet id) {
        BitSet app = (BitSet) me.id.clone();
        app.xor(id);
        final int pos = app.length() / 8;
        Klist list = null;
        ShortList res = new ShortList(this.k, this.me);
        for(int i = pos; res.size() < this.k && i < this.routing_table.size(); i++) {
            list = this.routing_table.get(i);
            if(list == null)
                continue;
            for(Contact c: list)
                res.add(c);
        }
        for(int i = pos - 1; res.size() < this.k && i >= 0; i--) {
            list = this.routing_table.get(i);
            if(list == null)
                continue;
            for(Contact c: list)
                res.add(c);
        }

        return res;
    }

    private void Lookup() {
        ShortList shortlist = new ShortList(this.k, this.me);

    }

    public void store() {}   // TODO sss
};
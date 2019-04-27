package kademlia;

import java.util.BitSet;

class Message {
    public enum kind {
        PING,
        FIND,
        STORE   //TODO
    };

    public final kind type;
    public final Contact sender;
    public final Contact receiver;

    public Message(Message.kind type, Contact sender, Contact receiver) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
    }

    public static class Response extends Message {
        // simply invert server and receiver of a message
        public Response(Message mes) {
            super(Message.kind.PING, mes.receiver, mes.sender);
        }
    };

    public static class FindRequest extends Message {
        public final BitSet id;
        public final ShortList traversed_nodes;
        public FindRequest(BitSet id, ShortList traversed_nodes, Contact sender, Contact receiver) {
            super(Message.kind.FIND, sender, receiver);
            this.id = id;
            this.traversed_nodes = traversed_nodes;
        }
    };

    public static class FindResponse extends Response {
        public final BitSet id;
        public final ShortList shortlist;
        public FindResponse(ShortList shortlist, Message mes) {
            super(mes);
            FindRequest req = (FindRequest) mes;
            this.id = req.id;
            this.shortlist = shortlist;
        }
    };
}
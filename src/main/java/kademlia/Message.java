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
    public final Contact reciver;

    public Message(Message.kind type, Contact sender, Contact reciver) {
        this.type = type;
        this.sender = sender;
        this.reciver = reciver;
    }

    public static class Response extends Message {
        // simply invert server and reciver of a message
        public Response(Message mes) {
            super(Message.kind.PING, mes.reciver, mes.sender);
        }
    };

    public static class FindRequest extends Message {
        public final BitSet id;
        public final ShortList traversed_nodes;
        public FindRequest(BitSet id, ShortList traversed_nodes, Contact sender, Contact reciver) {
            super(Message.kind.FIND, reciver, sender);
            this.id = id;
            this.traversed_nodes = traversed_nodes;
        }
    };

    public static class FindResponse extends Response {
        public final BitSet id;
        public final ShortList klist;
        public FindResponse(ShortList klist, Message mes) {
            super(mes);
            FindRequest req = (FindRequest) mes;
            this.id = req.id;
            this.klist = klist;
        }
    };
}
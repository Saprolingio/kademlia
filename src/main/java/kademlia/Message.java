package kademlia;

import java.util.BitSet;

/**
 * Messages used in kademlia. 
 * Only a few are actually implemented
 */
class Message {
    public enum kind {
        PING,
        FIND
    };

    public final kind type;
    public final Contact sender;
    public final Contact receiver;

    /**
     * Basic Message.
     * It can be used to ping.
     * @param type the type of message
     * @param sender the contact sending
     * @param receiver the contact reciving
     */
    public Message(Message.kind type, Contact sender, Contact receiver) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * Basic response Message.
     */
    public static class Response extends Message {
        /**
         * Utility to invert, sender and reciver
         * @param mes receiver a message to copy inverting sender and receiver
         */
        public Response(Message mes) {
            super(mes.type, mes.receiver, mes.sender);
        }
    };

    /**
     * FindRequest, used for FIND_NODE
     */
    public static class FindRequest extends Message {
        public final BitSet id;
        public final ShortList traversed_nodes;

        /**
         * This construct a FIND_NODE request
         * @param id the id to find
         * @param traversed_nodes node queried until now
         * @param sender    sender contact
         * @param receiver  receiver contact
         */
        public FindRequest(BitSet id, ShortList traversed_nodes, Contact sender, Contact receiver) {
            super(Message.kind.FIND, sender, receiver);
            this.id = id;
            this.traversed_nodes = traversed_nodes;
        }
    };

    /**
     * This class is the response to a FindRequest
     */
    public static class FindResponse extends Response {
        public final BitSet id;
        public final ShortList shortlist;

        /**
         * Create a Response given the parameters.
         * @param shortlist the list of element taken from the buckets
         * @param mes the FindRequest to which respond
         */
        public FindResponse(ShortList shortlist, Message mes) {
            super(mes);
            FindRequest req = (FindRequest) mes;
            this.id = req.id;
            this.shortlist = shortlist;
        }
    };
}
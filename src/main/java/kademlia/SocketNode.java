package kademlia;

import java.util.BitSet;
import java.util.Map;

/**
 * Socket Mockup
 */
class SocketNode {
    private Map<BitSet, Node> all_nodes; //!< A map to search the node to perform the fake send

    /**
     * Take in input a Map to perform the search of the destination of the send
     * @param all_nodes a Map containing all the reachable Node, indexed by their id
     */
    SocketNode(Map<BitSet, Node> all_nodes) {
        this.all_nodes = all_nodes;
    }

    /**
     * Send a message to the receiver if is reachable.
     * In this mockup the receiver is searched in the collection and is
     * called the receive function of the node is directly called.
     * @param mes message to be sended
     * @return if the node can be contacted (is present in the map), the resulting
     * message from the recive of the target node, otherwise null.
     */
    Message sendAndReceive(Message mes) {
        Node node = this.all_nodes.get(mes.receiver.id);
        if(node != null)
            return node.receive(mes);
        return null;
    }
}
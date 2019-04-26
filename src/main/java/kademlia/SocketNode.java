package kademlia;

import java.util.BitSet;
import java.util.Map;

class SocketNode {
    private Map<BitSet, Node> all_nodes;

    SocketNode(Map<BitSet, Node> all_nodes) {
        this.all_nodes = all_nodes;
    }

    Message sendAndReceive(Message mes) {
        Node node = this.all_nodes.get(mes.receiver.id);
        if(node != null)
            return node.receive(mes);
        return null;
    }
}
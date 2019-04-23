package kademlia;

import java.util.BitSet;
import java.util.Map;

class SocketNode {
    private Map<BitSet, Node> allnodes;

    SocketNode(Map<BitSet, Node> allnodes) {
        this.allnodes = allnodes;
    }

    Message sendAndRecive(Message mes) {
        Node node = this.allnodes.get(mes.reciver.id);
        if(node != null)
            return node.recive(mes);
        return null;
    }
}
package kademlia;

import java.util.ArrayDeque;

class Klist extends ArrayDeque<Contact> {
    private static final long serialVersionUID = 1L;
    private final int k; // maximum number of element in the Klist

    public Klist(int k){
        this.k = k;
    }

    public Contact addContact(Contact c) {
        if(this.remove(c))
            return c;    // need a ping to be refreshed

        if(this.size() < this.k) {
            this.addLast(c);
            return null;
        }

        return this.removeFirst(); // need a ping to be refreshed
    }

    public void refresh(Contact c) {
        if(this.remove(c) || this.size() < this.k)
            this.addLast(c);
    }
}
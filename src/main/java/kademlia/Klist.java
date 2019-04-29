package kademlia;

import java.util.ArrayDeque;

class Klist extends ArrayDeque<Contact> {
    private static final long serialVersionUID = 1L;
    private final int k; // maximum number of element in the Klist

    public Klist(int k){
        this.k = k;
    }

    Contact addContact(Contact c) {
        if(this.size() < this.k) {
            this.refresh(c);
            return c;
        }

        if(this.remove(c)) {
            this.addLast(c);
            return c;
        }

        return this.peek(); // need a ping to be refreshed
    }

    void refresh(Contact c) {
        this.remove(c);
        this.addLast(c);
    }

    void replace(Contact c) {
        this.removeFirst();
        this.addLast(c);
    }
}
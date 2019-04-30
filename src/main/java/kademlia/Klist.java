package kademlia;

import java.util.ArrayDeque;

/**
 * Very simple class with represent a bucket of Kademlia
 * it's called K list because it ensure that almost k element will be contained
 * it's very basic because ping function it's needed to trigger a refresh.
 */
class Klist extends ArrayDeque<Contact> {
    private static final long serialVersionUID = 1L;
    private final int k; /*!< maximum number of element in the Klist*/

    /**
     * The only constructor
     * @param k the maximum dimension of the container
     */
    public Klist(int k){
        // no super constructor called because not all bucket will reach dimension k
        this.k = k;
    }

    /**
     * Add a contact procedure of the bucket (some of it's preformed by the Node)
     * It add a Contact to the klist if:
     * * the contact it's not present, if it is it's removed because if this contact
     * is in the network must be moved to the tail
     * * else if the klist is not full, add it and report success using null
     * * otherwise return the first contact.
     *
     * calling it a first can add the element o provide a node to ping
     * if receive the pong you can recall di function with the returned element
     * otherwise you can add again the same element of the first call
     * @param c the contact you desire to add
     * @return the element passed if was present, null if was not present and if
     * it is inserted otherwise an older node to contact
     */
    public Contact addContact(Contact c) {
        if(this.remove(c))
            return c;    // need a ping to be refreshed

        if(this.size() < this.k) {
            this.addLast(c);
            return null;
        }

        return this.removeFirst(); // need a ping to be refreshed
    }

    /**
     * Refresh an already present (if any) contact, or add it if you can (used on receiving ping)
     * @param c contact to be refreshed
     */
    public void refresh(Contact c) {
        if(this.remove(c) || this.size() < this.k)
            this.addLast(c);
    }
}
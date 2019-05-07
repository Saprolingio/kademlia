package kademlia;

import java.util.ArrayList;
import java.util.BitSet;

/**
 * Contact wrapper to provide contact check.
 */
class Element {
    public final Contact contact;
    private boolean contacted;

    /**
     * Construct a non contacted element
     * @param contact the contact to be wrapped
     */
    public Element(Contact contact) {
        this.contact = contact;
        this.contacted = false;
    }

    /**
     * get the current status
     * @return contacted(true)/not contacted(false)
     */
    public boolean getContacted() {
        return this.contacted;
    }

    /**
     * the setter for the contact.
     * It can only set, because a contacted element cannot be "uncontacted".
     */
    public void setContacted() {
        this.contacted = true;
    }

    @Override
    public boolean equals(Object obj) {
        Element el = (Element) obj;
        // to be equal only the contact is needed to be equal
        return contact.equals(el.contact);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(contact);
        ret.append(", ");
        if(!this.contacted)
            ret.append("not ");
        ret.append("contacted");
        return ret.toString();
    }
};

/**
 * A extension of an ArrayList, provide merging, checked add and shrinking
 */
class ShortList extends ArrayList<Element>{
    private static final long serialVersionUID = 4193002791038011048L;
    private final int k;
    public final BitSet target; //!< the owner, used to sort the Shortlist

    /**
     * Shortlist shrinkable to k owned by owner
     * @param k shrinking dimension
     * @param target target of this shortlist
     */
    public ShortList(int k, Contact owner, BitSet target){
        this.k = k;
        this.ensureCapacity(k);
        this.target = target;
    }

    /**
     * Add the contact preventing insertion of duplicates
     * @param cont contact to add
     */
    public void add(Contact cont) {
        //checking for duplicates
        for(Element el: this) {
            if(el.contact.equals(cont))
                return;
        }
        this.add(new Element(cont));
    }

    /**
     * Add all element of another shortlist.
     * Using the other public method add, it prevent duplicated
     * @param list the list containing the elements to add. Can handle also
     * null list
     */
    public void addAll(ShortList list) {
        if(list == null)
            return;
        for(Element el: list)
            this.add(el.contact);
    }

    /**
     * Sort using the xor distance metric provided by Contact
     */
    public void sort() {
        this.sort((Element x, Element y) -> {
            if(x.contact.distance(target) > y.contact.distance(target))
                return 1; 
            if(x.contact.distance(target) < y.contact.distance(target))
                return -1;
            return 0; 
        });
    }

    /**
     * Merge 2 Shortlist.
     * The merge auto shirk the list to key. the selection fo the "closest"
     * to target 
     * @param list
     */
    public void merge(ShortList list) {
        if(list == null)
            return;
        this.addAll(list);
        this.sort();
        this.shrinkToK();
    }

    /**
     * Resize te Shortlist to k
     */
    public void shrinkToK() {
        if(this.k < this.size())
            this.removeRange(this.k, this.size());
    }

    /**
     * Get the first alpha not contacted Elements
     * @param alpha how many elements get
     * @return a list of alpha Elements
     */
    public ArrayList<Element> getAlpha(int alpha) {
        ArrayList<Element> ret = new ArrayList<Element>(alpha);
        for(Element e: this) {
            if(alpha == 0)
                break;
            if(!e.getContacted()) {
                ret.add(e);
                alpha--;
            }
        }
        return ret;
    }
}
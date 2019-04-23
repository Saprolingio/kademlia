package kademlia;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

class Element {
    public final Contact contact;
    public final boolean contacted;
    public Element(Contact contact) {
        this.contact = contact;
        this.contacted = false;
    }
};

class ShortList extends ArrayList<Element>{
    private final int k;
    private final Contact owner;

    public ShortList(int k, Contact owner){
        this.k = k;
        this.ensureCapacity(k);
        this.owner = owner;
    }

    public void add(Contact cont) {
        this.add(new Element(cont));
    }

    public void merge(ShortList list){
        this.addAll(list);
        this.sort((Element x, Element y) -> {
            long dist = x.contact.distance(owner) - y.contact.distance(owner);
            return (int)(dist % Integer.MAX_VALUE);
        });
        this.shrinkToK();
    }

    public void shrinkToK() {
        if(this.k < this.size())
            this.removeRange(this.k, this.size());
    }

    public ArrayList<Contact> getAlpha(int alpha) {
        ArrayList<Contact> ret = new ArrayList<Contact>(3);
        for(Element e: this) {
            if(!e.contacted) {
                ret.add(e.contact);
            }
        }
        return ret;
    }
}
package kademlia;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;

import javax.lang.model.util.ElementScanner6;

class Element {
    public final Contact contact;
    public final boolean contacted;
    public Element(Contact contact) {
        this.contact = contact;
        this.contacted = false;
    }

    public boolean equals(Object obj) {
        Element el = (Element) obj;
        // to be equal only the contact is needed to be equal
        return contact.equals(el.contact);
    }

    public String toString() {
        String str = contact.toString();
        str += ", ";
        if(!this.contacted)
            str += "not ";
        str += "contacted";
        return str;
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
        //find for duplicates
        for(Element el: this) {
            if(el.contact.equals(cont))
                return;
        }
        this.add(new Element(cont));
    }

    public void addAll(ShortList list) {
        for(Element el: list)
            this.add(el.contact);
    }

    public void sort() {
        this.sort((Element x, Element y) -> {
            long dist = x.contact.distance(owner) - y.contact.distance(owner);
            return (int)(dist % Integer.MAX_VALUE);
        });
    }

    public void merge(ShortList list) {
        this.addAll(list);
        this.sort();
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
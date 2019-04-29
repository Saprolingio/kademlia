package kademlia;

import java.util.ArrayList;

class Element {
    public final Contact contact;
    private boolean contacted;
    public Element(Contact contact) {
        this.contact = contact;
        this.contacted = false;
    }

    public boolean get_contacted() {
        return this.contacted;
    }

    public void set_contacted() {
        this.contacted = true;
    }

    public boolean equals(Object obj) {
        Element el = (Element) obj;
        // to be equal only the contact is needed to be equal
        return contact.equals(el.contact);
    }

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

class ShortList extends ArrayList<Element>{
    private static final long serialVersionUID = 4193002791038011048L;
    private final int k;
    public final Contact owner;

    public ShortList(int k, Contact owner){
        this.k = k;
        this.ensureCapacity(k);
        this.owner = owner;
    }

    public void add(Contact cont) {
        if(cont.equals(this.owner))
            return;
        //find for duplicates    
        for(Element el: this) {
            if(el.contact.equals(cont))
                return;
        }
        this.add(new Element(cont));
    }

    public void addAll(ShortList list) {
        if(list == null)
            return;
        for(Element el: list)
            this.add(el.contact);
    }

    public void sort() {
        this.sort((Element x, Element y) -> {
            if(x.contact.distance(owner) > y.contact.distance(owner))
                return 1; 
            if(x.contact.distance(owner) < y.contact.distance(owner))
                return -1;
            return 0; 
        });
    }

    public void merge(ShortList list) {
        if(list == null)
            return;
        this.addAll(list);
        this.sort();
        this.shrinkToK();
    }

    public void shrinkToK() {
        if(this.k < this.size())
            this.removeRange(this.k, this.size());
    }

    public ArrayList<Element> getAlpha(int alpha) {
        ArrayList<Element> ret = new ArrayList<Element>(alpha);
        for(Element e: this) {
            if(alpha == 0)
                break;
            if(!e.get_contacted()) {
                ret.add(e);
                alpha--;
            }
        }
        return ret;
    }
}
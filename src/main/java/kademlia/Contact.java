package kademlia;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.BitSet;
import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;

class Contact {
    public final InetAddress ip;
    public final BitSet id;
    public final int port; //used as unsigned int 16bit

    public Contact(InetAddress ip, int port, BitSet id) {
        this.ip = ip;
        this.port = port;
        this.id = id;
    }

    //auto assign an id based on ip port
    public Contact(InetAddress ip, int port, int id_length) throws UnsupportedEncodingException {
        this.ip = ip;
        this.id = Contact.hash(ip, port, id_length);
        this.port = port;
    }

    public String idString() {
        String ret = new String();
        for (byte b : this.id.toByteArray()) 
            ret += Integer.toHexString(b).toUpperCase();

        return ret;
    }

    public static BitSet hash(InetAddress ip, Integer port, int id_length) throws UnsupportedEncodingException {
        DigestSHA3 sha = new DigestSHA3(id_length);
        sha.update(ip.getAddress());
        sha.update(port.byteValue());
        BitSet res = new BitSet(id_length);
        res.valueOf(sha.digest());
        return res;
    }

    public long distance(Contact other) {
        BitSet self = (BitSet)this.id.clone();
        self.xor(other.id);
        long d = 0;
        for (long ndx : self.toLongArray())
            d += ndx;

        return d;
    }

    public boolean equals(Object obj) {
        Contact other = (Contact) obj;
        return this.id.equals(other.id) && this.ip.equals(other.ip) && this.port == other.port;
    }

    public String toString() {
        String ret = new String("{");
        ret += this.ip.toString();
        ret += ", ";
        ret += String.valueOf(port);
        ret += ", ";
        ret += this.idString();
        ret += "}";
        return ret;
    }
};
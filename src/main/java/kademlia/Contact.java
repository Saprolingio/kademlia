package kademlia;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.BitSet;
import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;

class Contact {
    public final InetAddress ip;
    public final BitSet id;
    public final int port; //used as unsigned int 16bit
    public final int id_bit_length;

    public Contact(InetAddress ip, int port, BitSet id, int id_bit_length) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.id_bit_length = id_bit_length;
    }

    //auto assign an id based on ip port
    public Contact(InetAddress ip, int port, int id_bit_length) throws UnsupportedEncodingException {
        this.ip = ip;
        this.id_bit_length = id_bit_length;
        this.id = Contact.hash(ip, port, id_bit_length);
        this.port = port;
    }

    public Contact(Contact to_clone) {
        this.ip = to_clone.ip;
        this.id = (BitSet) to_clone.id.clone();
        this.port = to_clone.port;
        this.id_bit_length = to_clone.id_bit_length;
    }

    public String idString() {
        String ret = new String();
        for (byte b : this.id.toByteArray())
            ret += Integer.toHexString(b).toUpperCase();

        if(ret.equals(""))
            ret += "0";

        return ret;
    }

    public static BitSet hash(InetAddress ip, Integer port, int id_length) throws UnsupportedEncodingException {
        DigestSHA3 sha;
        final int hash_len;
        if(id_length < 224)
            hash_len = 224;
        else if(id_length < 256)
            hash_len = 256;
        else if(id_length < 384)
            hash_len = 384;
        else
            hash_len = 512;
        sha = new DigestSHA3(hash_len);
        ByteBuffer buff = ByteBuffer.allocate(id_length);
        sha.update(ip.getAddress());
        sha.update(port.byteValue());
        byte b = 0;
        for(int i = 0; i < id_length; i += hash_len) {
            int min = Math.min(id_length, hash_len);
            buff.put(sha.digest(), 0, min);
            id_length -= min;
            sha.update(b++);
        }

        return BitSet.valueOf(buff);
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
package kademlia;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;

import static net.andreinc.mockneat.unit.types.Ints.ints;
import static net.andreinc.mockneat.unit.networking.IPv6s.ipv6s;


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
        this(ip, port, Contact.hash(ip, port, id_bit_length), id_bit_length);
    }

    //randomly generate contact
    public Contact(int id_bit_length) throws UnknownHostException, UnsupportedEncodingException {
        this(InetAddress.getByName(ipv6s().get()), ints().range(1024, 65535).get(), id_bit_length);
    }

    public Contact(Contact to_clone) {
        this.ip = to_clone.ip;
        this.id = (BitSet) to_clone.id.clone();
        this.port = to_clone.port;
        this.id_bit_length = to_clone.id_bit_length;
    }

    private static int idivCeil(int bitlen, int div) {
        return (int) Math.ceil((double)bitlen / div);
    }

    public String idString() {
        StringBuilder res = new StringBuilder();
        byte[] id_bytes = this.id.toByteArray();
        for(int i = idivCeil(this.id_bit_length, 16) - id_bytes.length * 2; i > 0; i--)
            res.append("0");
        for (byte b : id_bytes)
            res.append(String.format("%02X", b));
        return res.toString();
    }

    public String idByteString() {
        StringBuilder res = new StringBuilder();
        for(int i = this.id_bit_length; i > 0; i--)
            if(this.id.get(i))
                res.append("1");
            else
                res.append("0");
        return res.toString();
    }

    public static BitSet hash(InetAddress ip, Integer port, final int id_length) throws UnsupportedEncodingException {
        DigestSHA3 sha;
        int hash_len;
        if(id_length < 224)
            hash_len = 224;
        else if(id_length < 256)
            hash_len = 256;
        else if(id_length < 384)
            hash_len = 384;
        else
            hash_len = 512;
        sha = new DigestSHA3(hash_len);
        byte[] buff = new byte[idivCeil(id_length, 8)];
        sha.update(ip.getAddress());
        sha.update(port.byteValue());
        byte b = 0;
        final int remaining_byte = idivCeil(id_length, 8);
        for(int wrote = 0; wrote < remaining_byte;) {
            byte[] digest = sha.digest();
            for(int j = 0; j < digest.length && wrote < remaining_byte; j++) {
                buff[wrote] = digest[j];
                wrote++;
            }
            sha.update(b++);
        }
        BitSet bitset = BitSet.valueOf(buff);
        if(bitset.length() > id_length)
            bitset.clear(id_length, bitset.length());;
        return bitset;
    }

    public long distance(Contact other) {
        BitSet self = (BitSet)this.id.clone();
        self.xor(other.id);
        long d = 0;
        for (long ndx : self.toLongArray())
            d += ndx;

        return d;
    }

    @Override
    public boolean equals(Object obj) {
        Contact other = (Contact) obj;
        return this.id.equals(other.id);
    }

    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("{");
        ret.append(this.ip);
        ret.append(", ");
        ret.append(port);
        ret.append(", ");
        ret.append(this.idString());
        ret.append("}");
        return ret.toString();
    }
};
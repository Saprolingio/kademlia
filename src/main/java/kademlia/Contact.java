package kademlia;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;

import static net.andreinc.mockneat.unit.types.Ints.ints;
import static net.andreinc.mockneat.unit.networking.IPv6s.ipv6s;

/**
 * Kademlia contats. a triple <id, ip, port>
 */
class Contact {
    public final InetAddress ip;
    public final BitSet id;
    public final int port; //used as unsigned int 16bit
    public final int id_bit_length;

    /**
     * This is manly for Testing see the other constructor.
     * You must provide a valid Bitset id.
     * @param ip an IPV6/IPV4.
     * @param port port, please use in range [0-2^16] **no bound checking**
     * @param id Bitset a valid representing the id. It will **not** be trimmed to max bit_length.
     * @param id_bit_length max length of id
     */
    public Contact(InetAddress ip, int port, BitSet id, int id_bit_length) {
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.id_bit_length = id_bit_length;
    }

    /**
     * This constructor should be used in real application.
     * Generate an the id hashing the given ip and port (and bit_length)
     * @param ip an IPV6/IPV4.
     * @param port port, please use in range [0-2^16] **no bound checking**
     * @param id_bit_length max length of id
     * @throws UnsupportedEncodingException if hash cannot be performed, should never be thrown
     */
    public Contact(InetAddress ip, int port, int id_bit_length) throws UnsupportedEncodingException {
        this(ip, port, Contact.hash(ip, port, id_bit_length), id_bit_length);
    }

    /**
     * Randomly generate a Valid Contact.
     * Only max bit length is required.
     * @param id_bit_length max length of id
     * @throws UnknownHostException if generated ip is invalid, should never be thrown
     * @throws UnsupportedEncodingException if hash cannot be performed, should never be thrown
     */
    public Contact(int id_bit_length) throws UnknownHostException, UnsupportedEncodingException {
        this(InetAddress.getByName(ipv6s().get()), ints().range(1024, 65535).get(), id_bit_length);
    }

    /**
     * Copy constructor.
     * it copy the given contact cloning the id
     * @param to_clone
     */
    public Contact(Contact to_clone) {
        this.ip = to_clone.ip;
        this.id = (BitSet) to_clone.id.clone();
        this.port = to_clone.port;
        this.id_bit_length = to_clone.id_bit_length;
    }

    /**
     * Utility to do the integer division rounding up the result.
     * it's used to get byte/word needed to represent a bitset
     * @param bitlen    dividend
     * @param div       divisor
     * @return  quotient rounded up.
     */
    private static int idivCeil(int bitlen, int div) {
        return (int) Math.ceil((double)bitlen / div);
    }

    /**
     * Get a Hex string representation of the id.
     * @return an Hexadecimal String representing the id
     */
    public String idString() {
        StringBuilder res = new StringBuilder();
        byte[] id_bytes = this.id.toByteArray();
        for(int i = idivCeil(this.id_bit_length, 16) - id_bytes.length * 2; i > 0; i--)
            res.append("0");
        for(int i = id_bytes.length -1 ; i >= 0; i--)
            res.append(String.format("%02X", id_bytes[i]));
        return res.toString();
    }

    /**
     * Get a bit string representation of the id.
     * @return a bit String presenting the id.
     */
    public String idByteString() {
        StringBuilder res = new StringBuilder();
        for(int i = this.id_bit_length - 1; i >= 0; i--)
            if(this.id.get(i))
                res.append("1");
            else
                res.append("0");
        return res.toString();
    }

    /**
     * Return an hash based on the inputs.
     * @param ip an IPv4/IPv6
     * @param port port, please use in range [0-2^16] **no bound checking**
     * @param id_length max bit length of the id
     * @return a Bitset constructed with the hash of the ip + port
     * @throws UnsupportedEncodingException if the hash cannot be performed
     */
    public static BitSet hash(InetAddress ip, Integer port, final int id_length) throws UnsupportedEncodingException {
        DigestSHA3 sha;
        int hash_len;
        //selection the appropriate input for sha3
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
            sha.update(b++);    // changing something to generate different bytes
        }
        BitSet bitset = BitSet.valueOf(buff);
        if(bitset.length() > id_length) // clear the bit that will never be used
            bitset.clear(id_length, bitset.length());
        return bitset;
    }

    /**
     * Fundamental function to calculate the distance between a node and it's target.
     * it make use of the xor metric
     * @param id the contact with which calculate the distance
     * @return return the effective distance between 2 nodes
     */
    public long distance(BitSet id) {
        BitSet aux = (BitSet)this.id.clone();
        aux.xor(id);
        long d = 0;
        for (long ndx : aux.toLongArray())
            d += ndx;

        return d;
    }

    /**
     * Fundamental function to calculate the distance between 2 contacts.
     * it make use of the xor metric
     * @param other the contact with which calculate the distance
     * @return return the effective distance between 2 nodes
     */
    public long distance(Contact other) {
        return distance(other.id);
    }

    @Override
    public boolean equals(Object obj) {
        Contact other = (Contact) obj;
        return this.id.equals(other.id); //only the id it's sufficient to assert equality
    }

    @Override
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
package kademlia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class TestKademliaComponents
{
    final int id_bit_length = 32;

    @Test
    public void contact()
    {
        try {
            long[] longs = {3};
            BitSet hash = BitSet.valueOf(longs);
            Contact contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
            assertEquals("03", contact.idString());
            long[] longs1 = {2};
            hash = BitSet.valueOf(longs1);
            Contact contact1 = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
            assertEquals(1, contact.distance(contact1));
            assertEquals(contact.distance(contact), contact.distance(contact));

            String[] repr = {"01", "02", "04", "06", "08", "0A", "0C", "0E", "10", "12"};
            for(int i = 1; i < repr.length; i++) {
                longs[0] = i * 2;
                hash = BitSet.valueOf(longs);
                contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
                assertEquals(repr[i], contact.idString());
            }
        } catch(UnknownHostException e) {
            fail("impossible appened" + e.toString());
        }
    }

    
    @Test
    public void shortlist()
    {
        try {
            final int k = 20;
            long[] longs = {5};
            BitSet hash = BitSet.valueOf(longs);
            Contact owner = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
            ShortList shortlist = new ShortList(k, owner);
            final int start = 6;
            Contact contact;
            for(int i = start; i < (k + start + 1); i++) { //add k+1 elements
                longs[0] = i;
                hash = BitSet.valueOf(longs);
                contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
                shortlist.add(contact);
            }

            assertEquals(shortlist.size(), k + 1);
            int i = start;
            for (Element element : shortlist) {
                longs[0] = i;
                hash = BitSet.valueOf(longs);
                contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
                assertEquals(contact, element.contact);
                assertFalse(element.get_contacted());
                i++;
            }

            shortlist.shrinkToK();
            assertEquals(k, shortlist.size());
            shortlist.sort();

            //test merging with more distant elements
            ShortList distant = new ShortList(k, owner);
            longs[0] = 99999999;
            hash = BitSet.valueOf(longs);
            contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
            distant.add(contact);
            distant.add(new Contact(contact));
            assertEquals(1, distant.size());    //test for duplicates element
            Element el = shortlist.get(shortlist.size() - 1);
            shortlist.merge(distant);
            assertEquals(k, shortlist.size());
            assertEquals(el, shortlist.get(shortlist.size() - 1));

            //test merging more closer elements
            distant.clear();
            long[] longs1 = {4};
            hash = BitSet.valueOf(longs1);
            contact = new Contact(InetAddress.getByName("192.168.0.1"), 123, hash, id_bit_length);
            distant.add(contact);
            el = shortlist.get(0);
            shortlist.merge(distant);
            assertEquals(k, shortlist.size());
            assertNotEquals(el, shortlist.get(0));
            assertEquals(new Element(contact), shortlist.get(0));
        } catch(UnknownHostException e) {
            fail("impossible happened" + e.toString());
        }
    }
}

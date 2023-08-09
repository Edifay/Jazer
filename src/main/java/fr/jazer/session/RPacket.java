package fr.jazer.session;

import fr.jazer.session.stream.TaggedEntity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This class is used to receive data from a {@link Session}.
 */
public class RPacket implements TaggedEntity {
    /**
     * RPacket represent the ID of a packet, it can be recognized by this number.
     */
    protected int packetNumber;
    /**
     * The data contained by the Packet as ByteArray.
     */
    protected byte[] data;

    /**
     * Constructor used by a Session when reading a new packet.
     *
     * @param packetNumber the packetNumber representing the ID of the packet.
     * @param data         the data contained by the Packet as ByteArray.
     */
    public RPacket(final int packetNumber, final byte[] data) {
        this.packetNumber = packetNumber;
        this.data = data;
    }

    /**
     * @return the packetNumber of the RPacket.
     */
    @Override
    public int getTag() {
        return this.packetNumber;
    }

    /**
     * @return the data contained by the packet.
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * @return use the {@link RPacket#data} to parse a String encoded with UTF_8.
     */
    public String readString() {
        return new String(this.data, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "RPacket{" +
                "packetNumber=" + packetNumber +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}

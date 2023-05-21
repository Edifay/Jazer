package fr.jazer.session;

import fr.jazer.session.stream.TaggedEntity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * This class is used to send data through a {@link Session}.
 */
public class SPacket implements TaggedEntity {
    /**
     * RPacket represent the ID of a packet, it can be recognized by this number.
     */
    protected int packetNumber;
    /**
     * The data contained by the Packet as ByteArray.
     */
    protected byte[] data;

    /**
     * Use this constructor to create a packet with empty data.
     *
     * @param packetNumber the packetNumber representing the ID of the packet.
     */
    public SPacket(final int packetNumber) {
        this.packetNumber = packetNumber;
    }

    /**
     * Use this constructor to create a packet with data.
     *
     * @param packetNumber the packetNumber representing the ID of the packet.
     * @param data         the data contained by the Packet as ByteArray.
     */
    public SPacket(final int packetNumber, final byte[] data) {
        this.packetNumber = packetNumber;
        this.data = data;
    }

    /**
     * @return the current data owned by this SPacket.
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Used to set the data of the current SPacket.
     *
     * @param data the data to used.
     * @return the current SPacket, to be used as a Builder.
     */
    public SPacket writeBytes(final byte[] data) {
        this.data = data;
        return this;
    }

    /**
     * Used to save a String in this packet, encoded with UTF_8.
     *
     * @param text the text to encode.
     * @return the current SPacket, to be used as a Builder.
     */
    public SPacket writeString(final String text) {
        this.data = text.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    /**
     * @return the packetNumber of the RPacket.
     */
    @Override
    public int getTag() {
        return this.packetNumber;
    }

    @Override
    public String toString() {
        return "SPacket{" +
                "packetNumber=" + packetNumber +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}

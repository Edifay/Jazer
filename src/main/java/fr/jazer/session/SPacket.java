package fr.jazer.session;

import fr.jazer.session.flux.TaggedEntity;

import java.nio.charset.StandardCharsets;

public class SPacket implements TaggedEntity {

    protected int packetNumber;
    protected byte[] data;

    public SPacket(final int packetNumber) {
        this.packetNumber = packetNumber;
    }

    public SPacket(final int packetNumber, final byte[] data) {
        this.packetNumber = packetNumber;
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }

    public SPacket writeBytes(final byte[] data) {
        this.data = data;
        return this;
    }

    public SPacket writeString(final String text) {
        this.data = text.getBytes(StandardCharsets.UTF_8);
        return this;
    }

    @Override
    public int getTag() {
        return this.packetNumber;
    }
}

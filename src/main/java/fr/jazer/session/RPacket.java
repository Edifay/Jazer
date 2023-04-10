package fr.jazer.session;

import fr.jazer.session.flux.TaggedEntity;

import java.nio.charset.StandardCharsets;

public class RPacket implements TaggedEntity {

    protected int packetNumber;
    protected byte[] data;

    public RPacket(final int packetNumber, final byte[] data) {
        this.packetNumber = packetNumber;
        this.data = data;
    }

    @Override
    public int getTag() {
        return this.packetNumber;
    }

    public byte[] getData() {
        return this.data;
    }

    public String readString(){
        return new String(data, StandardCharsets.UTF_8);
    }
}

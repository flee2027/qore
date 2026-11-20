package jake2.qcommon.network.messages.server;

import jake2.qcommon.Defines;
import jake2.qcommon.sizebuf_t;

import java.util.Collection;
import java.util.Set;

import static jake2.qcommon.Defines.TE_FIREFLY;

public class FireflyTEMessage extends TEMessage {
    public static final Collection<Integer> SUBTYPES = Set.of(
            TE_FIREFLY
    );

    public float[] origin = new float[3];
    public int count;
    public int radius;

    public FireflyTEMessage() {
        super(Defines.TE_FIREFLY);
    }

    public FireflyTEMessage(float[] origin, int count, int radius) {
        this();
        System.arraycopy(origin, 0, this.origin, 0, 3);
        this.count = count;
        this.radius = radius;
    }

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        super.writeProperties(buffer);

        buffer.writeCoord(origin[0]);
        buffer.writeCoord(origin[1]);
        buffer.writeCoord(origin[2]);

        buffer.writeByte((byte) count);
        buffer.writeShort((short) radius);
    }

    @Override
    public void parse(sizebuf_t buffer) {
        origin[0] = buffer.readCoord();
        origin[1] = buffer.readCoord();
        origin[2] = buffer.readCoord();
        count = buffer.readByte();
        radius = buffer.readShort();
    }

    @Override
    public int getSize() {
        // style(1) + origin(3 * 2 байта для coord) + count(1) + radius(2) = 10 байт
        // (Если используешь writeFloat/readFloat для координат, размер будет 1 + 12 + 1 + 2 = 16 байт)
        return 10;
    }

    @Override
    Collection<Integer> getSupportedStyles() {
        return SUBTYPES;
    }

    @Override
    public String toString() {
        return "FireflyTEMessage{count=" + count + ", radius=" + radius + "}";
    }
}
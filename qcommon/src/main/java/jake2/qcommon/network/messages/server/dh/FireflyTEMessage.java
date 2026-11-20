package jake2.qcommon.network.messages.server.dh;

import jake2.qcommon.Defines;
import jake2.qcommon.network.messages.server.ServerMessage;
import jake2.qcommon.network.messages.server.ServerMessageType;
import jake2.qcommon.sizebuf_t;

public class FireflyTEMessage extends ServerMessage {
    public int subtype;
    public float[] origin = new float[3];
    public int count;
    public int radius;

    public FireflyTEMessage() {
        super(ServerMessageType.svc_temp_entity);
    }

    public FireflyTEMessage(int subtype, float[] origin, int count, int radius) {
        this();
        this.subtype = subtype;
        System.arraycopy(origin, 0, this.origin, 0, 3);
        this.count = count;
        this.radius = radius;
    }

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        buffer.writeByte((byte) subtype);

        buffer.writeCoord(origin[0]);
        buffer.writeCoord(origin[1]);
        buffer.writeCoord(origin[2]);

        buffer.writeByte((byte) count);
        buffer.writeShort((short) radius);
    }

    @Override
    public void parse(sizebuf_t buffer) {
        this.subtype = buffer.readByte();
        this.origin[0] = buffer.readCoord();
        this.origin[1] = buffer.readCoord();
        this.origin[2] = buffer.readCoord();
        this.count = buffer.readByte();
        this.radius = buffer.readShort();
    }

    @Override
    public int getSize() {
        return 16;
    }
}
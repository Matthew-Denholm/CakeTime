package me.c0wg0d.caketime;

import java.io.IOException;

import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;
import net.minecraft.server.v1_7_R4.PacketListener;

public class PacketPlayOutWorldBorder extends Packet {
    private Action action;
    private double radius = 0;
    private double oldradius = 0;
    private long speed = 0;
    private double x = 0;
    private double z = 0;
    private int warningTime = 0;
    private int warningBlocks = 0;
    private int portalBoundary = 0;

    public PacketPlayOutWorldBorder() {
    }

    public PacketPlayOutWorldBorder(Action action) {
        this.setAction(action);
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.action = Action.values()[packetdataserializer.a()];
        switch (this.action) {
        case SET_SIZE:
            this.radius = packetdataserializer.readDouble();
            break;
        case LERP_SIZE:
            this.oldradius = packetdataserializer.readDouble();
            this.radius = packetdataserializer.readDouble();
            this.speed = packetdataserializer.readLong();
            break;
        case SET_CENTER:
            this.x = packetdataserializer.readDouble();
            this.z = packetdataserializer.readDouble();
            break;
        case INITIALIZE:
            this.x = packetdataserializer.readDouble();
            this.z = packetdataserializer.readDouble();
            this.oldradius = packetdataserializer.readDouble();
            this.radius = packetdataserializer.readDouble();
            this.speed = packetdataserializer.readLong();
            this.portalBoundary = packetdataserializer.readInt();
            this.warningTime = packetdataserializer.readInt();
            this.warningBlocks = packetdataserializer.readInt();
            break;
        case SET_WARNING_TIME:
            this.warningTime = packetdataserializer.readInt();
            break;
        case SET_WARNING_BLOCKS:
            this.warningBlocks = packetdataserializer.readInt();
            break;
        default:
            break;
        }
    }

    public void b(PacketDataSerializer serializer) {
        serializer.b(this.action.ordinal());
        switch (action) {
        case SET_SIZE: {
            serializer.writeDouble(this.radius);
            break;
        }
        case LERP_SIZE: {
            serializer.writeDouble(this.oldradius);
            serializer.writeDouble(this.radius);
            serializer.b((int) this.speed);
            break;
        }
        case SET_CENTER: {
            serializer.writeDouble(this.x);
            serializer.writeDouble(this.z);
            break;
        }
        case SET_WARNING_BLOCKS: {
            serializer.b(this.warningBlocks);
            break;
        }
        case SET_WARNING_TIME: {
            serializer.b(this.warningTime);
            break;
        }
        case INITIALIZE: {
            serializer.writeDouble(this.x);
            serializer.writeDouble(this.z);
            serializer.writeDouble(this.oldradius);
            serializer.writeDouble(this.radius);
            serializer.b((int) this.speed);
            serializer.b(this.portalBoundary);
            serializer.b(this.warningBlocks);
            serializer.b(this.warningTime);
        }
        }
    }

    public void handle(PacketListener packetlistener) {
    }

    public int getPortalBoundary() {
        return portalBoundary;
    }

    public void setPortalBoundary(int portalBoundary) {
        this.portalBoundary = portalBoundary;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getOldradius() {
        return oldradius;
    }

    public void setOldradius(double oldradius) {
        this.oldradius = oldradius;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public int getWarningBlocks() {
        return warningBlocks;
    }

    public void setWarningBlocks(int warningBlocks) {
        this.warningBlocks = warningBlocks;
    }

    public int getWarningTime() {
        return warningTime;
    }

    public void setWarningTime(int warningTime) {
        this.warningTime = warningTime;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public static enum Action {
        SET_SIZE, LERP_SIZE, SET_CENTER, INITIALIZE, SET_WARNING_TIME, SET_WARNING_BLOCKS;

        private Action() {
        }
    }
}

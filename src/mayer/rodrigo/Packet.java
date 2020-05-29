package mayer.rodrigo;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class Packet {

    private int seqNum;
    private String data;

    public Packet (int seqNum, String data) {
        this.seqNum = seqNum;
        this.data = data;
    }

    // Usado por pacotes de fim de transmissão e ACKs
    public Packet (int seqNum) {
        this(seqNum, "");
    }

    // Usado para criar um pacote a partir de um DatagramPacket recebido
    public Packet(DatagramPacket datagramPacket, byte[] buffer) {
        String received = new String(datagramPacket.getData(), datagramPacket.getOffset(), buffer.length);
        String[] parsedReceived = received.split(";");

        this.seqNum = Integer.parseInt(parsedReceived[0]);
        this.data = parsedReceived[1];
    }

    public int getSeqNum() {
        return seqNum;
    }

    public String getData() {
        return data;
    }

    public DatagramPacket getDatagramPacket(InetAddress ipAddress, int port) {
        byte[] dataToSend = (seqNum + ";" + data).getBytes();
        return new DatagramPacket(dataToSend, dataToSend.length, ipAddress, port);
    }

    @Override
    public String toString() {
        return seqNum + ";" + data;
    }
}

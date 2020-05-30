package mayer.rodrigo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {

    public static final int PORT = 3334;

    private static DatagramSocket receiverSocket;
    private static InetAddress ipAddress;

    private static int lastSeqNum;
    private static boolean sendAckCalled = false;

    public static void main(String[] args) throws Exception {

        receiverSocket = new DatagramSocket(PORT);
        ipAddress = InetAddress.getByName("127.0.0.1");

        System.out.println("Receiver inicializado na porta " + PORT + "...");

        while (true) {
            listenToTransmission();
        }

    }

    private static void listenToTransmission() throws Exception {
        String message = "";
        lastSeqNum = -1;
        boolean endTransmission = false;

        System.out.println("Aguardando transmissão");

        while (!endTransmission) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            receiverSocket.receive(packet);

            Packet receivedPacket = new Packet(packet, buffer);

            int seqNum = receivedPacket.getSeqNum();
            String partialData = receivedPacket.getData();

            if (seqNum == 0) {
                System.out.println("Início da transmissão");
            }

            if (seqNum == lastSeqNum + 1 && partialData.trim().equals("EOF")) {
                // Fim da transmissão
                endTransmission = true;
                lastSeqNum = seqNum;
                System.out.println("Pacote recebido: " + receivedPacket.toString());
                // Enviar ACK do último pacote
                Packet ackPacket = new Packet(lastSeqNum);
                receiverSocket.send(ackPacket.getDatagramPacket(ipAddress, Sender.PORT));
                sendAckCalled = false;

                System.out.println("ACK " + ackPacket.getSeqNum() + " enviado");
                System.out.println("Mensagem completa:");
                System.out.println(message);
                System.out.println("Fim da transmissão");
                System.out.println();
            } else {
                if (seqNum == lastSeqNum + 1) {
                    System.out.println("Pacote recebido: " + receivedPacket.toString());
                    lastSeqNum = seqNum;
                    message += partialData + " ";
                } else if (seqNum > lastSeqNum) {
                    System.out.println("Pacote fora de ordem descartado: " + receivedPacket.toString());
                } else {
                    System.out.println("Pacote duplicado descartado: " + receivedPacket.toString());
                }
                // Enviar ACK com o último número de sequência recebido
                sendAck();
            }
        }
    }

    private static void sendAck() throws Exception {

        if (sendAckCalled) {
            return;
        }

        // Aguardar 200 ms para enviar o ACK cumulativo dos últimos pacotes recebidos
        setTimeout(() -> {
            try {
                if (lastSeqNum == -1) { // Previne o envio de mais ACKs após o término de uma transmissão
                    return;
                }
                Packet ackPacket = new Packet(lastSeqNum);
                receiverSocket.send(ackPacket.getDatagramPacket(ipAddress, Sender.PORT));
                System.out.println("ACK " + ackPacket.getSeqNum() + " enviado");
                sendAckCalled = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 200);

        sendAckCalled = true;
    }

    // https://stackoverflow.com/a/36842856/8856946
    public static void setTimeout(Runnable runnable, int delay) throws Exception {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }
}

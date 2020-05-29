package mayer.rodrigo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {

    public static final int PORT = 3334;

    private static DatagramSocket receiverSocket;
    private static InetAddress ipAddress;

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
        int lastSeqNum = -1;
        boolean endTransmission = false;

        System.out.println("Aguardando transmissão");

        while (!endTransmission) {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            receiverSocket.receive(packet);

            String data = new String(packet.getData(), packet.getOffset(), buffer.length);
            String[] parsedData = data.split(";");

            int seqNum = Integer.parseInt(parsedData[0]);
            String partialData = parsedData[1];

            if (seqNum == 0) {
                System.out.println("Início da transmissão");
            }

            if (seqNum == -1) {
                // Fim da transmissão
                endTransmission = true;
                lastSeqNum = seqNum;
                System.out.println("Mensagem completa:");
                System.out.println(message);
                System.out.println("Fim da transmissão");
            } else if (seqNum == lastSeqNum + 1) {
                System.out.println("Pacote recebido: " + data);
                lastSeqNum = seqNum;
                message += partialData + " ";
            } else if (seqNum > lastSeqNum) {
                System.out.println("Pacote fora de ordem descartado: " + data);
            } else {
                System.out.println("Pacote duplicado descartado: " + data);
            }

            // Enviar ACK com o último número de sequência recebido
            sendAck(lastSeqNum);
        }
    }

    private static void sendAck(Integer seqNum) throws Exception {
        byte[] data = (seqNum + ";").getBytes();
        receiverSocket.send(new DatagramPacket(data, data.length, ipAddress, Sender.PORT));
    }
}

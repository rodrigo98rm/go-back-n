package mayer.rodrigo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;

public class Sender {

    public static final int PORT = 3333;

    private static DatagramSocket senderSocket;
    private static InetAddress ipAddress;
    private static Scanner scanner = new Scanner(System.in);
    private static final int window = 4;

    public static void main(String[] args) throws Exception {

        // Iniciar socket
        senderSocket = new DatagramSocket(PORT);
        ipAddress = InetAddress.getByName("127.0.0.1");

        boolean run = true;
        boolean restart;

        System.out.println("Sender inicializado na porta " + PORT + "...");

        while (run) {
            System.out.println("Insira a mensagem que deseja enviar e pressioner Enter ⏎:");
            String data = scanner.nextLine();

            int result = showMenu();

            switch (result) {
                case 1:
                    System.out.println("Enviar normal");
                    normalSend(data);
                    break;
                case 2:
                    System.out.println("Enviar lento");
                    break;
                case 3:
                    System.out.println("Enviar com perda");
                    break;
                case 4:
                    System.out.println("Enviar fora de ordem");
                    break;
                case 5:
                    System.out.println("Enviar duplicado");
                    break;
                default:
                    System.out.println("Opção inválida");
            }

            restart = restart();
            if (!restart) {
                run = false;
            }
        }
    }

    private static int showMenu() {

        System.out.println("Selecione o método de envio (insira o número da opção):");
        System.out.println("1 - Envio normal");
        System.out.println("2 - Envio lento");
        System.out.println("3 - Envio com perda");
        System.out.println("4 - Envio fora de ordem");
        System.out.println("5 - Envio duplicado");
        String option = scanner.nextLine();

        return Integer.parseInt(option);
    }

    private static boolean restart() {
        System.out.println("Deseja enviar outra mensagem? S/N");
        String restart = scanner.nextLine();

        return restart.equalsIgnoreCase("S");
    }

    private static void normalSend(String data) throws Exception {

        // Separar data em pacotes, uma palavra por pacote
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        String[] parts = data.split(" ");
        for (int i = 0; i < parts.length; i++) {
            // Pacote com formato "seqNum;data"
            byte[] dataToSend = (i + ";" + parts[i]).getBytes();
            packets.add(new DatagramPacket(dataToSend, dataToSend.length, ipAddress, Receiver.PORT));
        }

        // Chamar funcao para envio de pacotes
        startTransmission(packets);
    }

    private static void startTransmission(ArrayList<DatagramPacket> packets) throws Exception {

        System.out.println("Transmissão iniciada");

        // Adicionar pacote de fim de transmissão
        byte[] endData = "-1;".getBytes();
        packets.add(new DatagramPacket(endData, endData.length, ipAddress, Receiver.PORT));

        int base = 0;
        int nextSeqNum = 0;

        // Enquanto a base não atingir o último pacote (-1)
        while (base != -1) {
            // Enviar os pacotes dentro da janela de envio
            for (int i = nextSeqNum; i < base + window && i < packets.size(); i++) {
                sendPacket(packets.get(i));
                System.out.println("Pacote enviado: " + i);
                nextSeqNum++;
            }

            // Nova transmissão realizada -> reiniciar o timeout
            senderSocket.setSoTimeout(3000);

            // Aguardar um ACK ou timeout
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                senderSocket.receive(packet);

                String received = new String(packet.getData(), packet.getOffset(), buffer.length);
                String[] parsedReceived = received.split(";");

                int receivedSeqNum = Integer.parseInt(parsedReceived[0]);

                if (receivedSeqNum == -1) {
                    // ACK recebido do último pacote -> fim da transmissão
                    System.out.println("ACK de fim de transmissão recebido: " + receivedSeqNum);
                    base = receivedSeqNum;
                } else if (receivedSeqNum >= base) {
                    // ACK esperado recebido -> mover a base, voltar ao início do loop
                    System.out.println("ACK " + receivedSeqNum);
                    base = receivedSeqNum + 1;
                } else {
                    // ACK duplicado recebido -> não faz nada
                    System.out.println("ACK duplicado recebido: " + receivedSeqNum);
                }
            } catch (SocketTimeoutException e) {
                // Timeout estourado -> definir nextSeqNum como base e voltar ao início do loop (reenviar pacotes)
                System.out.println("Timeout estourado, reenviando...");
                nextSeqNum = base;
            }
        }
    }

    private static void sendPacket(DatagramPacket packet) throws Exception {
        senderSocket.send(packet);
    }
}
package mayer.rodrigo;

import java.io.IOException;
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

    private static ArrayList<Packet> packets;
    private static final int OPTION_NORMAL = 1, OPTION_DELAY = 2, OPTION_LOSS = 3, OPTION_ORDER = 4, OPTION_DUPLICATE = 5;
    private static int option;

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
                    option = OPTION_NORMAL;
                    System.out.println("Enviar normal");
                    break;
                case 2:
                    option = OPTION_DELAY;
                    System.out.println("Enviar lento");
                    break;
                case 3:
                    option = OPTION_LOSS;
                    System.out.println("Enviar com perda");
                    break;
                case 4:
                    option = OPTION_ORDER;
                    System.out.println("Enviar fora de ordem");
                    break;
                case 5:
                    option = OPTION_DUPLICATE;
                    System.out.println("Enviar duplicado");
                    break;
                default:
                    System.out.println("Opção inválida");
            }

            normalSend(data);

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
        packets = new ArrayList<>();
        String[] parts = data.split(" ");
        for (int i = 0; i < parts.length; i++) {
            // Pacote com formato "seqNum;data"
            packets.add(new Packet(i, parts[i]));
        }

        // Chamar funcao para envio de pacotes
        startTransmission();
    }

    private static void startTransmission() throws Exception {

        System.out.println("Transmissão iniciada");

        // Adicionar pacote de fim de transmissão
        packets.add(new Packet(packets.size(), "EOF"));

        int base = 0;
        int nextSeqNum = 0;

        // Enquanto a base não atingir o último pacote + 1
        while (base != packets.size()) {
            // Enviar os pacotes dentro da janela de envio
            for (int i = nextSeqNum; i < base + window && i < packets.size(); i++) {
                Packet packet = packets.get(i);
                sendPacket(packet);
                nextSeqNum++;
            }

            // Nova transmissão realizada -> reiniciar o timeout
            senderSocket.setSoTimeout(3000);

            // Aguardar um ACK ou timeout
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                senderSocket.receive(packet);

                Packet receivedPacket = new Packet(packet, buffer);
                int receivedSeqNum = receivedPacket.getSeqNum();

                if (receivedSeqNum == packets.size() - 1) {
                    // ACK recebido do último pacote -> fim da transmissão
                    System.out.println("ACK de fim de transmissão recebido: " + receivedSeqNum);
                    System.out.println("Transmissão finalizada");
                    System.out.println();
                    base = receivedSeqNum + 1;
                } else if (receivedSeqNum >= base) {
                    // ACK esperado recebido -> mover a base, voltar ao início do loop
                    System.out.println("ACK " + receivedSeqNum);
                    base = receivedSeqNum + 1;
                } else {
                    // ACK duplicado recebido -> não faz nada
                    System.out.println("ACK duplicado recebido: " + receivedSeqNum + " - Ignorado");
                }
            } catch (SocketTimeoutException e) {
                // Timeout estourado -> definir nextSeqNum como base e voltar ao início do loop (reenviar pacotes)
                System.out.println("Timeout estourado, reenviando...");
                nextSeqNum = base;
            }
        }
    }

    // Utiliza um pacote do meio das mensagens para simular erros
    // Todos os outros pacotes são enviados normalmente
    private static void sendPacket(Packet packet) throws Exception {

        int middle = Math.floorDiv(packets.size(), 2);

        // Simular opção de pacotes fora de ordem
        if(option == OPTION_ORDER) {
            if(packet.getSeqNum() == middle) {
                // Enviar pacote X + 1 no lugar do X
                senderSocket.send(packets.get(middle + 1).getDatagramPacket(ipAddress, Receiver.PORT));
                System.out.println("Pacote enviado FORA DE ORDEM: " + packets.get(middle + 1).getSeqNum());
                return;
            } else if (packet.getSeqNum() == middle + 1) {
                // Enviar pacote X no lugar do X + 1
                senderSocket.send(packets.get(middle).getDatagramPacket(ipAddress, Receiver.PORT));
                System.out.println("Pacote enviado FORA DE ORDEM: " + packets.get(middle).getSeqNum());
                option = OPTION_NORMAL; // Retorna a config normal para enviar o pacote corretamente depois
                return;
            }
        }

        // Caso não seja o pacote do meio, enviar normalmente
        if (packet.getSeqNum() != middle) {
            senderSocket.send(packet.getDatagramPacket(ipAddress, Receiver.PORT));
            System.out.println("Pacote enviado: " + packet.getSeqNum());
            return;
        }

        // Pacote do meio -> Simular problema
        switch (option) {
            case OPTION_DELAY:
                setTimeout(() -> {
                    try {
                        senderSocket.send(packet.getDatagramPacket(ipAddress, Receiver.PORT));
                        System.out.println("Pacote enviado com atraso: " + packet.getSeqNum());
                        option = OPTION_NORMAL; // Retorna a config normal para enviar o pacote corretamente depois
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, 100);
                break;
            case OPTION_LOSS:
                // Não enviar o pacote
                System.out.println("Pacote PERDIDO: " + packet.getSeqNum());
                option = OPTION_NORMAL; // Retorna a config normal para enviar o pacote corretamente depois
                break;
            case OPTION_DUPLICATE:
                senderSocket.send(packet.getDatagramPacket(ipAddress, Receiver.PORT));
                System.out.println("Pacote enviado: " + packet.getSeqNum());
                senderSocket.send(packet.getDatagramPacket(ipAddress, Receiver.PORT));
                System.out.println("Pacote enviado duplicado: " + packet.getSeqNum());
                break;
            default:
                senderSocket.send(packet.getDatagramPacket(ipAddress, Receiver.PORT));
                System.out.println("Pacote enviado: " + packet.getSeqNum());
                break;
        }
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
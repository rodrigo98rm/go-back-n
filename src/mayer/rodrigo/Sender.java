package mayer.rodrigo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Scanner;

public class Sender {

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {

        boolean run = true;
        boolean restart;

        while (run) {
            System.out.println("Insira a mensagem que deseja enviar e pressioner Enter ⏎:");
            String data = scanner.nextLine();
            System.out.println(data);

            int result = showMenu();

            switch(result) {
                case 1:
                    System.out.println("Enviar normal");
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

        //        System.out.println("Sender initialized...");
//
//        DatagramSocket senderSocket = new DatagramSocket();
//        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
//
//        byte[] data = new byte[1024];
//        data = "Hello World!".getBytes();
//
//        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, 3334);
//        senderSocket.send(packet);
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

        // Iniciar socket
        DatagramSocket senderSocket = new DatagramSocket();
        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");

        // Separar em pacotes
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        String[] parts = data.split(" ");
        for (String part : parts) {
            byte[] dataToSend = part.getBytes();
            packets.add(new DatagramPacket(dataToSend, dataToSend.length, ipAddress, 3334));
        }

        // Montar vetor de pacotes

        // Chamar funcao para envio de pacotes
    }

    private static void sendPacket(DatagramSocket socket, DatagramPacket packet) throws Exception {
        socket.send(packet);
    }
}

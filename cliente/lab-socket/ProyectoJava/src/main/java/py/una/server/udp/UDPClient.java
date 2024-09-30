package py.una.server.udp;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient {

    public static void main(String[] args) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        String direccionServidor = inFromUser.readLine();

        System.out.print("Ingrese su puerto local: ");
        int puertoLocal = Integer.parseInt(inFromUser.readLine());

        System.out.print("Ingrese su nombre de usuario: ");
        String nombreUsuario = inFromUser.readLine();

        DatagramSocket clientSocket = new DatagramSocket(puertoLocal);
        InetAddress IPAddress = InetAddress.getByName(direccionServidor);

        System.out.println("Iniciando conexión al servidor en " + direccionServidor);
        int puertoServidor = 9876;

        // Enviar la dirección IP, puerto y nombre de usuario al servidor
        String registro = InetAddress.getLocalHost().getHostAddress() + ":" + puertoLocal + ":" + nombreUsuario;
        byte[] sendData = registro.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, puertoServidor);
        clientSocket.send(sendPacket);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Hilo para enviar mensajes
        pool.execute(() -> {
            try {
                while (true) {
                    System.out.print("Ingrese su mensaje: ");
                    String mensaje = inFromUser.readLine();
                    byte[] datosAEnviar = mensaje.getBytes();
                    DatagramPacket paqueteEnvio = new DatagramPacket(datosAEnviar, datosAEnviar.length, IPAddress, puertoServidor);
                    clientSocket.send(paqueteEnvio);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Hilo para recibir mensajes como servidor
        pool.execute(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);

                    String mensaje = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    String usuarioOrigen = receivePacket.getAddress().toString() + ":" + receivePacket.getPort();
                    System.out.println("Mensaje recibido de " + usuarioOrigen + ": " + mensaje);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

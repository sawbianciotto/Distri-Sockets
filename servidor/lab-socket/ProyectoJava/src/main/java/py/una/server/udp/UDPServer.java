package py.una.server.udp;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class UDPServer {

    private static Map<String, InetSocketAddress> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int puertoServidor = 9876;

        try {
            DatagramSocket serverSocket = new DatagramSocket(puertoServidor);
            System.out.println("Servidor escuchando en el puerto " + puertoServidor);
            ExecutorService pool = Executors.newCachedThreadPool();

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                pool.execute(new ClientHandler(serverSocket, receivePacket));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    static class ClientHandler implements Runnable {
        private DatagramSocket socket;
        private DatagramPacket packet;

        public ClientHandler(DatagramSocket socket, DatagramPacket packet) {
            this.socket = socket;
            this.packet = packet;
        }

        @Override
        public void run() {
            try {
                String mensajeRecibido = new String(packet.getData(), 0, packet.getLength());
                InetSocketAddress clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());

                // Registrar al cliente con su IP/puerto/nombre de usuario
                String[] partes = mensajeRecibido.split(":", 3);
                if (partes.length == 3) {
                    String usuario = partes[2].trim();
                    clients.put(usuario, clientAddress);
                    System.out.println("Cliente conectado: " + usuario + " - " + clientAddress);
                } else {
                    String usuarioOrigen = getUsernameByAddress(clientAddress);
                    if (usuarioOrigen != null) {
                        reenviarMensajeGrupal(usuarioOrigen, mensajeRecibido);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getUsernameByAddress(InetSocketAddress address) {
            return clients.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(address))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }

        private void reenviarMensajeGrupal(String usuario, String mensaje) throws IOException {
            String mensajeFormateado = usuario + ": " + mensaje;
            byte[] sendData = mensajeFormateado.getBytes();
            for (InetSocketAddress clientAddress : clients.values()) {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress.getAddress(), clientAddress.getPort());
                socket.send(sendPacket);
            }
            System.out.println("Mensaje enviado por " + usuario + ": " + mensaje);
        }
    }
}

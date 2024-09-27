package py.una.server.udp;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class UDPServer {

    // Mapa para almacenar los clientes conectados. La clave es el nombre del usuario y el valor es su dirección InetSocketAddress (IP y puerto).
    private static Map<String, InetSocketAddress> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int puertoServidor = 9876; // Puerto donde el servidor estará escuchando

        try {
            // Crear el socket del servidor UDP en el puerto especificado
            DatagramSocket serverSocket = new DatagramSocket(puertoServidor);
            System.out.println("Servidor de Chat UDP escuchando en el puerto " + puertoServidor);

            // Crear un pool de hilos para manejar múltiples clientes simultáneamente
            ExecutorService pool = Executors.newCachedThreadPool();

            // Bucle infinito para recibir paquetes continuamente
            while (true) {
                byte[] receiveData = new byte[1024]; // Arreglo de bytes para almacenar los datos recibidos
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // Paquete UDP para recibir datos
                serverSocket.receive(receivePacket); // Espera a recibir un paquete del cliente

                // Asignar la tarea de procesar el paquete a un nuevo hilo en el pool
                pool.execute(new ClientHandler(serverSocket, receivePacket));
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // Imprimir cualquier excepción que ocurra
            System.exit(1); // Salir en caso de error grave
        }
    }

    // Clase que maneja a cada cliente conectado
    static class ClientHandler implements Runnable {
        private DatagramSocket socket; // Socket del servidor para enviar respuestas
        private DatagramPacket packet; // Paquete recibido del cliente

        public ClientHandler(DatagramSocket socket, DatagramPacket packet) {
            this.socket = socket; // Inicializa el socket
            this.packet = packet; // Inicializa el paquete recibido
        }

        @Override
        public void run() {
            try {
                // Extraer el mensaje recibido como cadena de texto
                String mensajeRecibido = new String(packet.getData(), 0, packet.getLength());
                InetAddress IPAddress = packet.getAddress(); // Obtener la dirección IP del cliente
                int port = packet.getPort(); // Obtener el puerto del cliente
                InetSocketAddress clientAddress = new InetSocketAddress(IPAddress, port); // Crear un objeto que representa la dirección del cliente

                // Si el cliente no está registrado, el primer mensaje será su nombre de usuario
                if (!clients.containsValue(clientAddress)) {
                    // Registrar al cliente con su nombre de usuario
                    clients.put(mensajeRecibido, clientAddress);
                    System.out.println("Nuevo cliente conectado: " + mensajeRecibido + " desde " + IPAddress + ":" + port);
                    enviarMensajeATodos("Servidor", mensajeRecibido + " se ha unido al chat."); // Notificar a todos los clientes que un nuevo usuario se ha unido
                } else {
                    // Si ya está registrado, procesar el mensaje normal, que incluye destinatario y contenido
                    String[] parts = mensajeRecibido.split(":", 2); // Dividir el mensaje en destinatario y contenido (separado por ':')
                    if (parts.length == 2) { // Verificar que el formato sea correcto (destinatario: mensaje)
                        String destinatario = parts[0].trim(); // Obtener el destinatario
                        String mensaje = parts[1].trim(); // Obtener el contenido del mensaje
                        String usuario = getUsernameByAddress(clientAddress); // Obtener el nombre de usuario que corresponde a la dirección del cliente

                        if (usuario != null && clients.containsKey(destinatario)) { // Si el usuario y destinatario existen
                            enviarMensajeAUsuario(usuario, destinatario, mensaje); // Enviar el mensaje al destinatario
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(); // Imprimir cualquier excepción que ocurra
            }
        }

        // Método para obtener el nombre de usuario a partir de su dirección (IP y puerto)
        private String getUsernameByAddress(InetSocketAddress address) {
            for (Map.Entry<String, InetSocketAddress> entry : clients.entrySet()) { // Recorre la lista de clientes registrados
                if (entry.getValue().equals(address)) { // Si encuentra la dirección coincide con la buscada
                    return entry.getKey(); // Retorna el nombre del usuario
                }
            }
            return null; // Si no encuentra, retorna null
        }

        // Método para enviar un mensaje a un usuario específico
        private void enviarMensajeAUsuario(String usuarioOrigen, String destinatario, String mensaje) throws IOException {
            String mensajeFormateado = usuarioOrigen + ": " + mensaje; // Formatear el mensaje para que incluya el nombre del usuario origen
            byte[] sendData = mensajeFormateado.getBytes(); // Convertir el mensaje a bytes

            // Obtener la dirección del destinatario
            InetSocketAddress clientAddress = clients.get(destinatario);
            if (clientAddress != null) { // Si el destinatario está conectado
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress.getAddress(), clientAddress.getPort());
                // Crear un paquete con el mensaje y la dirección del destinatario
                socket.send(sendPacket); // Enviar el paquete al destinatario
            } else {
                System.out.println("El usuario destinatario " + destinatario + " no está conectado."); // Si no se encuentra el destinatario, imprimir un mensaje en el servidor
            }
        }

        // Método para enviar un mensaje a todos los usuarios conectados (broadcast)
        private void enviarMensajeATodos(String usuario, String mensaje) throws IOException {
            String mensajeFormateado = usuario + ": " + mensaje; // Formatear el mensaje con el usuario origen
            byte[] sendData = mensajeFormateado.getBytes(); // Convertir el mensaje a bytes
            for (InetSocketAddress clientAddress : clients.values()) { // Recorrer todos los clientes conectados
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress.getAddress(), clientAddress.getPort());
                socket.send(sendPacket); // Enviar el paquete a cada cliente
            }
        }
    }
}

package py.una.server.udp;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClient {

    public static void main(String[] args) throws Exception {

        // Dirección IP del servidor
        String direccionServidor = "127.0.0.1"; // Dirección de loopback para conectarse al localhost
        int puertoServidor = 9876; // Puerto donde el servidor escucha
        String nombreUsuario; // Variable para almacenar el nombre de usuario del cliente

        // Buffer para leer entradas del usuario (teclado)
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        // Leer el nombre del usuario
        System.out.print("Ingrese su nombre de usuario: ");
        nombreUsuario = inFromUser.readLine(); // Guardar el nombre de usuario ingresado por teclado

        // Crear el socket UDP para el cliente
        DatagramSocket clientSocket = new DatagramSocket(); // Crea un socket en cualquier puerto disponible en el cliente

        // Obtener la dirección IP del servidor a partir de la cadena de texto
        InetAddress IPAddress = InetAddress.getByName(direccionServidor);
        System.out.println("Conectando al servidor...");

        // Enviar el nombre de usuario al servidor (primer mensaje)
        byte[] sendData = nombreUsuario.getBytes(); // Convierte el nombre del usuario a un arreglo de bytes
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, puertoServidor);
        // Crea un paquete de datos con el nombre de usuario, la dirección del servidor y el puerto del servidor
        clientSocket.send(sendPacket); // Envía el paquete al servidor

        // Crear un grupo de hilos (thread pool) para manejar el envío y recepción de mensajes
        ExecutorService pool = Executors.newFixedThreadPool(2); // Usa dos hilos: uno para enviar y otro para recibir mensajes

        // Hilo para enviar mensajes
        pool.execute(() -> { // Crea un nuevo hilo para enviar mensajes
            try {
                while (true) {
                    // Leer el mensaje del usuario y el destinatario
                    System.out.print("Ingrese destinatario y mensaje (formato: usuario: mensaje): ");
                    String mensaje = inFromUser.readLine(); // Lee el mensaje que incluye destinatario y contenido del mensaje

                    // Convertir el mensaje a bytes para enviarlo por el socket
                    byte[] datosAEnviar = mensaje.getBytes(); // Convierte el mensaje a un arreglo de bytes
                    // Crear un paquete con el mensaje y la información del servidor (IP y puerto)
                    DatagramPacket paqueteEnvio = new DatagramPacket(datosAEnviar, datosAEnviar.length, IPAddress, puertoServidor);
                    clientSocket.send(paqueteEnvio); // Envía el paquete al servidor
                }
            } catch (IOException e) {
                e.printStackTrace(); // Imprime cualquier error relacionado con la entrada/salida
            }
        });

        // Hilo para recibir mensajes
        pool.execute(() -> { // Crea un nuevo hilo para recibir mensajes
            try {
                byte[] receiveData = new byte[1024]; // Arreglo de bytes para recibir datos del servidor (máximo 1024 bytes por mensaje)
                while (true) {
                    // Crear un paquete vacío para recibir datos
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket); // Espera a recibir un paquete del servidor

                    // Convertir el contenido del paquete recibido a una cadena de texto
                    String mensaje = new String(receivePacket.getData(), 0, receivePacket.getLength()); // Obtiene los datos recibidos y los convierte a texto
                    System.out.println(mensaje); // Muestra el mensaje recibido en la consola del cliente
                }
            } catch (IOException e) {
                e.printStackTrace(); // Imprime cualquier error relacionado con la entrada/salida
            }
        });
    }
}






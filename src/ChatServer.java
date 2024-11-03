import java.io.*; // Importa las clases necesarias para la entrada/salida de datos.
import java.net.*; // Importa las clases necesarias para trabajar con sockets de red.
import java.time.LocalDateTime; // Importa LocalDateTime para obtener la fecha y hora actuales.
import java.time.format.DateTimeFormatter; // Importa DateTimeFormatter para dar formato a la fecha y hora.
import java.util.List; // Importa la interfaz List para manejar una lista de clientes conectados.
import java.util.concurrent.CopyOnWriteArrayList; // Importa CopyOnWriteArrayList, una implementación segura de lista para concurrencia.

public class ChatServer {
    // Define el puerto en el que el servidor escuchará las conexiones de los clientes.
    private static final int PORT = 2024;

    // Lista de flujos de salida de los clientes conectados. CopyOnWriteArrayList permite modificar la lista de forma segura en un entorno concurrente.
    private static List<PrintWriter> activeClients = new CopyOnWriteArrayList<>();

    // Formato para mostrar la fecha y la hora en los mensajes de la terminal del servidor.
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Método principal del servidor donde se inicia la espera de conexiones.
    public static void main(String[] args) {
        // Muestra un mensaje en la terminal indicando que el servidor está en línea.
        System.out.println("El servidor de chat está en línea y esperando conexiones...");

        // Intenta crear un socket de servidor en el puerto especificado.
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Bucle infinito para aceptar conexiones de clientes.
            while (true) {
                // Espera a que un cliente se conecte y acepta la conexión.
                Socket clientSocket = serverSocket.accept();

                // Crea un PrintWriter para enviar mensajes al cliente.
                PrintWriter clientOutput = new PrintWriter(clientSocket.getOutputStream(), true);

                // Crea un BufferedReader para leer mensajes enviados por el cliente.
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Solicita el nombre de usuario al cliente y lo envía a través del flujo de salida.
                clientOutput.println("Por favor, ingresa tu nombre de usuario:");

                // Lee el nombre de usuario proporcionado por el cliente.
                String username = clientInput.readLine();

                // Muestra en la terminal del servidor que un nuevo cliente se ha conectado, incluyendo la IP y el nombre del usuario.
                logMessage("Conexión establecida con: " + clientSocket.getInetAddress() + " - Usuario: " + username);

                // Agrega el flujo de salida del cliente a la lista de clientes activos.
                activeClients.add(clientOutput);

                // Inicia un nuevo hilo para manejar la sesión del cliente de manera concurrente.
                new ChatSession(clientSocket, clientOutput, clientInput, username).start();
            }
        } catch (IOException e) {
            // En caso de error al iniciar el servidor, muestra el mensaje de error en la terminal.
            logMessage("Error en el servidor: " + e.getMessage());
        }
    }

    /**
     * Método para registrar mensajes en la terminal con fecha y hora.
     * @param message El mensaje que se mostrará en la terminal.
     */
    private static void logMessage(String message) {
        // Obtiene la fecha y hora actuales en el formato especificado.
        String timestamp = LocalDateTime.now().format(formatter);

        // Muestra el mensaje en la terminal, incluyendo la fecha y hora.
        System.out.println("[" + timestamp + "] " + message);
    }

    /**
     * Clase interna para manejar la sesión de un cliente conectado.
     * Extiende Thread para manejar la conexión de cada cliente en un hilo separado.
     */
    static class ChatSession extends Thread {
        private Socket clientSocket; // Socket del cliente conectado.
        private BufferedReader clientInput; // Flujo de entrada para leer mensajes del cliente.
        private PrintWriter clientOutput; // Flujo de salida para enviar mensajes al cliente.
        private String username; // Nombre de usuario del cliente.

        /**
         * Constructor de la clase ChatSession.
         * @param clientSocket Socket del cliente.
         * @param clientOutput Flujo de salida del cliente.
         * @param clientInput Flujo de entrada del cliente.
         * @param username Nombre de usuario del cliente.
         */
        public ChatSession(Socket clientSocket, PrintWriter clientOutput, BufferedReader clientInput, String username) {
            this.clientSocket = clientSocket;
            this.clientOutput = clientOutput;
            this.clientInput = clientInput;
            this.username = username;
        }

        /**
         * Método principal que se ejecuta en el hilo de la sesión del cliente.
         * Escucha y maneja los mensajes del cliente.
         */
        @Override
        public void run() {
            try {
                // Mensaje de bienvenida al unirse al chat, que se envía a todos los clientes.
                String joinMessage = username + " se ha unido al chat.";
                sendToAll(joinMessage);

                // Bucle para leer mensajes del cliente en tiempo real.
                String message;
                while ((message = clientInput.readLine()) != null) {
                    // Si el cliente envía un mensaje indicando su salida, informa a todos y sale del bucle.
                    if (message.equalsIgnoreCase(username + " ha salido del chat.")) {
                        sendToAll(username + " ha dejado el chat.");
                        logMessage(username + " se ha desconectado.");
                        break;
                    }
                    // Envía el mensaje del cliente a todos los clientes conectados.
                    sendToAll(username + ": " + message);
                }
            } catch (IOException e) {
                // En caso de desconexión inesperada, muestra un mensaje de error en la terminal.
                logMessage("El cliente " + username + " se ha desconectado inesperadamente.");
            } finally {
                // Cuando el cliente se desconecta, se elimina su flujo de salida de la lista de clientes activos.
                activeClients.remove(clientOutput);

                // Intenta cerrar el socket del cliente para liberar recursos.
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // En caso de error al cerrar el socket, muestra un mensaje en la terminal.
                    logMessage("Error al cerrar el socket del cliente: " + e.getMessage());
                }
            }
        }

        /**
         * Método para enviar un mensaje a todos los clientes conectados.
         * @param message El mensaje que se enviará a todos los clientes.
         */
        private void sendToAll(String message) {
            // Itera sobre cada cliente conectado en la lista de flujos de salida.
            for (PrintWriter writer : activeClients) {
                // Envía el mensaje a cada cliente.
                writer.println(message);
            }
        }
    }
}

import java.io.*; // Importa las clases necesarias para la entrada/salida de datos.
import java.net.*; // Importa las clases necesarias para trabajar con sockets de red.
import java.util.Random; // Importa la clase Random para generar nombres aleatorios si el usuario no ingresa uno.


public class MessageClient {
    // Método principal del cliente que establece la conexión y gestiona la comunicación con el servidor.
    public static void main(String[] args) {
        // Usa try-with-resources para asegurar que la entrada de la consola se cierre automáticamente.
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {

            // Solicita al usuario la dirección IP del servidor.
            System.out.print("Introduce la dirección IP del servidor, por defecto: localhost: ");
            String serverAddress = consoleInput.readLine().trim();
            if (serverAddress.isEmpty()) serverAddress = "localhost"; // Si no se ingresa una dirección, usa "localhost".

            // Solicita al usuario el puerto del servidor.
            System.out.print("Introduce el puerto del servidor, por defecto PORT: 2024: ");
            String portInput = consoleInput.readLine().trim();
            int port = portInput.isEmpty() ? 2024 : Integer.parseInt(portInput); // Usa 2024 si el usuario no ingresa un puerto.

            // Solicita el nombre de usuario que se mostrará en el chat.
            System.out.print("Introduce tu nombre de usuario: ");
            String username = consoleInput.readLine().trim();

            // Si el usuario no ingresa un nombre, se le asigna uno automáticamente llamando a generateRandomName.
            if (username.isEmpty()) {
                username = generateRandomName();
                System.out.println("No ingresaste un nombre. Se te asignará el nombre de usuario: " + username);
            }

            // Intenta establecer una conexión con el servidor y gestiona la comunicación.
            try (Socket socket = new Socket(serverAddress, port);
                 BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true)) {

                // Envía el nombre de usuario al servidor.
                serverOutput.println(username);
                System.out.println("Conectado al servidor de mensajes en " + serverAddress + ":" + port);

                // Crea un hilo para escuchar mensajes del servidor sin bloquear la entrada del usuario.
                Thread serverListener = new Thread(new ServerMessageListener(serverInput));
                serverListener.start();

                // Bucle principal para enviar mensajes al servidor.
                String userMessage;
                while ((userMessage = consoleInput.readLine()) != null) {
                    // Detecta si el usuario quiere salir con los comandos "exit", "adios" o "chao".
                    if (userMessage.equalsIgnoreCase("exit") ||
                            userMessage.equalsIgnoreCase("adios") ||
                            userMessage.equalsIgnoreCase("chao")) {

                        // Pregunta si el usuario confirma la salida.
                        System.out.print("¿Deseas salir del chat? (s/n): ");
                        String confirmExit = consoleInput.readLine().trim().toLowerCase();

                        // Si el usuario confirma la salida con "s", envía un mensaje de desconexión al servidor y cierra el chat.
                        if ("s".equals(confirmExit)) {
                            serverOutput.println(username + " ha salido del chat.");
                            System.out.println("Te has desconectado del chat.");
                            break; // Sale del bucle para finalizar la conexión.
                        } else if ("n".equals(confirmExit)) {
                            // Si el usuario elige "n", permanece en el chat.
                            System.out.println("Continuando en el chat...");
                        } else {
                            // Mensaje para manejar opciones no válidas.
                            System.out.println("Opción no válida. Escribe 's' para salir o 'n' para continuar.");
                        }
                    } else {
                        // Envía un mensaje normal al servidor.
                        serverOutput.println(userMessage);
                    }
                }

            } catch (IOException e) {
                // Maneja cualquier error que ocurra al intentar conectar con el servidor.
                System.err.println("Error al conectar con el servidor: " + e.getMessage());
            }
        } catch (IOException e) {
            // Maneja errores en la lectura de la entrada de la consola.
            System.err.println("Error al leer la entrada del usuario: " + e.getMessage());
        }
    }

    /**
     * Método para generar un nombre divertido aleatorio si el usuario no ingresa un nombre.
     * @return Un nombre divertido seleccionado aleatoriamente de la lista.
     */
    private static String generateRandomName() {
        String[] funnyNames = {
                "GatoSaltarin", "ZorroFeliz", "PandaTravieso", "LoboSabio", "ElefanteBailarin",
                "ConejoLoco", "TortugaRapida", "LeonDormilon", "OsoJugueton", "PulpoIngenioso"
        };
        Random random = new Random();
        return funnyNames[random.nextInt(funnyNames.length)]; // Selecciona un nombre aleatorio de la lista.
    }

    /**
     * Clase interna para escuchar mensajes enviados desde el servidor en un hilo separado.
     * Implementa Runnable para que pueda ejecutarse en un hilo.
     */
    static class ServerMessageListener implements Runnable {
        private BufferedReader serverInput; // Flujo de entrada para leer mensajes del servidor.

        // Constructor que inicializa el flujo de entrada desde el servidor.
        public ServerMessageListener(BufferedReader serverInput) {
            this.serverInput = serverInput;
        }

        /**
         * Método run que se ejecuta en un hilo separado para leer y mostrar mensajes del servidor en tiempo real.
         */
        @Override
        public void run() {
            try {
                String message;
                // Lee mensajes del servidor de forma continua y los muestra en la consola.
                while ((message = serverInput.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                // Muestra un mensaje cuando se termina la conexión con el servidor.
                System.out.println("Conexión con el servidor finalizada.");
            }
        }
    }
}

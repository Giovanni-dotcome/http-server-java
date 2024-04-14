import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void writeNotFound(OutputStream outputStream) throws IOException {
        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }

    public static void writeBody(OutputStream outputStream, String contentType, String content) throws IOException {
        outputStream.write(("HTTP/1.1 200 OK\r\nContent-Type: "+ contentType + "\r\nContent-Length: " + content.length() + "\r\n\r\n" + content).getBytes());

    }

    public static String getDir(String[] args) {
        for (int i = 0; i < args.length; ++i)
            if (args[i].equals("--directory") && ((i + 1) < args.length)) return args[i+1];
        return null;
    }

    public static void handleClient(Socket clientSocket, String[] args) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {
            String[] requestLine = bufferedReader.readLine().split(" ");
            String method = requestLine[0];
            String target = requestLine[1];
            String dir = getDir(args);

            if (target.equals("/")) outputStream.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
            else if (target.equals("/user-agent")) {
                bufferedReader.readLine();
                String content = bufferedReader.readLine().split("User-Agent: ")[1];
                writeBody(outputStream, "text/plain", content);
            }
            else if (target.contains("/files/") && dir != null) {
                String fileName = target.split("/files/")[1];
                File file = Paths.get(dir, fileName).toFile();

                if (method.equals("GET") && file.exists()) writeBody(outputStream, "application/octet-stream", Files.readString(file.toPath()));
                else if (method.equals("POST") && file.createNewFile()) {
                    String line = bufferedReader.readLine();
                    StringBuilder content = new StringBuilder();

                    while (line != null) line = bufferedReader.readLine();
                    while (bufferedReader.ready()) content.append(bufferedReader.read());

                    PrintWriter writer = new PrintWriter(file);
                    writer.print(content);
                    writer.close();

                    outputStream.write(("HTTP/1.1 201 OK\r\n\r\n").getBytes());
                } else writeNotFound(outputStream);

            }
            else if (target.contains("/echo/")) {
                String content = target.split("/echo/")[1];
                writeBody(outputStream, "text/plain", content);
            } else writeNotFound(outputStream);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");


        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // arrow function that instantiate the functional interface Runnable (since Runnable have only 1 abstract method is functional, and therefor the compiler interprets the arrow function as an implementation of this abstract method)
                new Thread(() -> handleClient(clientSocket, args)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}

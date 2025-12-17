
package demo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SimpleHttpServer {

    static Properties props = new Properties();
    static Path outDir;

    public static void main(String[] args) throws Exception {
        try (InputStream is = SimpleHttpServer.class
                .getClassLoader().getResourceAsStream("application.properties")) {
            props.load(is);
        }

        int port = Integer.parseInt(props.getProperty("port", "8080"));
        outDir = Paths.get(props.getProperty("out.dir", "./out"));
        Files.createDirectories(outDir);

        ServerSocket server = new ServerSocket(port);
        System.out.println("Listening on " + port);

        while (true) {
            Socket s = server.accept();
            handle(s);
        }
    }

    static void handle(Socket s) {
        try (s) {
            InputStream in = s.getInputStream();
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                raw.write(buf, 0, read);
                if (raw.toString().contains("\r\n\r\n")) break;
            }

            String req = raw.toString(StandardCharsets.UTF_8);
            saveRaw(req);

            OutputStream out = s.getOutputStream();

            if (req.startsWith("GET")) {
                byte[] body = Files.readAllBytes(
                        Paths.get("sample-form.html"));
                writeResponse(out, "200 OK", "text/html", body);
            } else if (req.startsWith("POST")) {
                String body = req.substring(req.indexOf("\r\n\r\n") + 4);
                if (!body.contains("name=") || body.contains("name=&")) {
                    writeResponse(out, "400 Bad Request",
                            "text/plain", "name missing".getBytes());
                } else {
                    writeResponse(out, "200 OK",
                            "text/plain", "OK".getBytes());
                }
            } else {
                writeResponse(out, "405 Method Not Allowed",
                        "text/plain", ("Not allowed : " + req).getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void writeResponse(OutputStream out, String status,
                              String type, byte[] body) throws IOException {
        String hdr = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: " + type + "\r\n" +
                "Content-Length: " + body.length + "\r\n\r\n";
        out.write(hdr.getBytes());
        out.write(body);
    }

    static void saveRaw(String req) throws IOException {
        String ts = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .replace(":", "-");
        String name = (req.startsWith("GET") ? "GET" : "POST")
                + "_" + ts + "_" + new Random().nextInt(90) + ".txt";
        Files.writeString(outDir.resolve(name), req);
    }
}

package demo;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class RequestSaver {
  
  /**
   * Saves HTTP request to a file with naming convention: VERB_timestamp_random.txt
   * For example: GET_2025-12-17T23-27-00.022_45.txt
   */
  public static Path saveText(String httpVerb, String content, Path outDir) throws IOException {
    Files.createDirectories(outDir);
    String timestamp = LocalDateTime.now()
        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .replace(":", "-");
    String filename = httpVerb + "_" + timestamp + "_" + new Random().nextInt(90) + ".txt";
    Path file = outDir.resolve(filename);
    Files.writeString(file, content);
    System.err.println("[SAVED] " + filename);
    return file;
  }
  
  /**
   * Extracts the HTTP verb from the request string.
   * Returns the verb (GET, POST, PATCH, etc.) or "UNKNOWN" if not parseable.
   */
  public static String extractVerb(String request) {
    if (request == null || request.isEmpty()) return "UNKNOWN";
    String firstLine = request.split("\\r?\\n")[0];
    String[] parts = firstLine.split(" ");
    return parts.length > 0 ? parts[0] : "UNKNOWN";
  }
}

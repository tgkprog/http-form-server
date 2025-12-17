package demo;
import java.io.*;
import java.nio.file.*;

public class MultipartPostHandler {
  
  /**
   * Handles multipart POST requests.
   * Creates request.txt and extracts uploaded files to attachments/ subfolder.
   * @param rawRequest The full HTTP request
   * @param folder The folder where request.txt and attachments/ will be created
   */
  public static void handle(String rawRequest, Path folder) throws IOException {
    Files.createDirectories(folder);
    Files.createDirectories(folder.resolve("attachments"));
    
    // Save full request as request.txt
    Files.writeString(folder.resolve("request.txt"), rawRequest);
    System.err.println("[MULTIPART] Created folder: " + folder.getFileName());
    
    // Parse and save attachments
    parseAndSaveAttachments(rawRequest, folder.resolve("attachments"));
  }
  
  private static void parseAndSaveAttachments(String rawRequest, Path attachmentsDir) throws IOException {
    // Extract boundary from Content-Type header
    String boundary = extractBoundary(rawRequest);
    if (boundary == null) {
      System.err.println("[MULTIPART] No boundary found, skipping attachment parsing");
      return;
    }
    
    // Split by boundary
    String[] parts = rawRequest.split("--" + boundary.replace("+", "\\+"));
    
    int fileIndex = 0;
    for (String part : parts) {
      if (part.trim().isEmpty() || part.contains("--")) continue;
      
      // Check if this part contains a file
      if (part.contains("Content-Disposition:") && part.contains("filename=")) {
        String filename = extractFilename(part);
        byte[] fileContent = extractFileContent(part);
        
        if (filename != null && fileContent != null && fileContent.length > 0) {
          Path filePath = attachmentsDir.resolve(filename);
          Files.write(filePath, fileContent);
          System.err.println("[MULTIPART] Saved attachment: " + filename + " (" + fileContent.length + " bytes)");
          fileIndex++;
        }
      }
    }
    
    if (fileIndex == 0) {
      System.err.println("[MULTIPART] No attachments found in request");
    }
  }
  
  private static String extractBoundary(String request) {
    for (String line : request.split("\\r?\\n")) {
      if (line.toLowerCase().startsWith("content-type:") && line.contains("boundary=")) {
        int idx = line.indexOf("boundary=");
        String boundary = line.substring(idx + 9).trim();
        // Remove quotes if present
        if (boundary.startsWith("\"")) boundary = boundary.substring(1);
        if (boundary.endsWith("\"")) boundary = boundary.substring(0, boundary.length() - 1);
        return boundary;
      }
    }
    return null;
  }
  
  private static String extractFilename(String part) {
    for (String line : part.split("\\r?\\n")) {
      if (line.contains("filename=")) {
        int start = line.indexOf("filename=\"");
        if (start == -1) start = line.indexOf("filename=");
        if (start != -1) {
          start = line.indexOf("\"", start) + 1;
          int end = line.indexOf("\"", start);
          if (end > start) {
            return line.substring(start, end);
          }
        }
      }
    }
    return "unnamed_file_" + System.currentTimeMillis();
  }
  
  private static byte[] extractFileContent(String part) {
    // Find the blank line that separates headers from content
    int contentStart = part.indexOf("\r\n\r\n");
    if (contentStart == -1) contentStart = part.indexOf("\n\n");
    if (contentStart == -1) return null;
    
    contentStart += (part.contains("\r\n\r\n") ? 4 : 2);
    
    // Content goes until the end of this part (might have trailing \r\n)
    String content = part.substring(contentStart);
    content = content.replaceAll("\\r?\\n$", ""); // Remove trailing newlines
    
    return content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }
}

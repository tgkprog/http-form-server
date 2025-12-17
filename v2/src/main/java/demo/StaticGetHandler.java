
package demo;
import java.io.*;
import java.nio.file.*;

public class StaticGetHandler {
  Path www;
  
  public StaticGetHandler(Path w, Path out) {
    www = w;
  }
  
  public void handle(String path, OutputStream out) throws IOException {
    if(path.equals("/")) path = "/index.html";
    
    // Check if this is a directory request (no . in the path)
    boolean isDirectoryRequest = !path.contains(".");
    
    // Remove leading / and resolve path
    String requestedPath = path.startsWith("/") ? path.substring(1) : path;
    Path p = www.resolve(requestedPath).normalize();
    
    // Security check: ensure resolved path is still under www directory
    if(!p.normalize().startsWith(www.normalize())) {
      System.err.println("[STATIC] Security violation: " + path);
      write(out, "403 Forbidden", "text/plain", "Forbidden");
      return;
    }
    
    if(!Files.exists(p)) {
      System.err.println("[STATIC] 404: " + path);
      
      // If it's a directory request (no . in path), show directory listing
      if(isDirectoryRequest) {
        handleDirectoryListing(path, out);
        return;
      }
      
      // Otherwise just return 404
      write(out, "404 Not Found", "text/plain", "Not found");
      return;
    }
    
    // If path exists and is a directory, show listing
    if(Files.isDirectory(p)) {
      handleDirectoryListing(path, out);
      return;
    }
    
    // Serve file
    try {
      byte[] d = Files.readAllBytes(p);
      String mimeType = mime(p.toString());
      System.err.println("[STATIC] 200: " + path + " (" + mimeType + ", " + d.length + " bytes)");
      write(out, "200 OK", mimeType, d);
    } catch (Exception e) {
      System.err.println("[STATIC] Error reading file: " + path + " - " + e.getMessage());
      write(out, "500 Internal Server Error", "text/plain", "Error reading file");
    }
  }
  
  private void handleDirectoryListing(String requestPath, OutputStream out) throws IOException {
    // Remove leading slash and any trailing slashes
    String dirPath = requestPath.equals("/") ? "" : requestPath.substring(1).replaceAll("/$", "");
    Path targetDir = dirPath.isEmpty() ? www : www.resolve(dirPath).normalize();
    
    if(!targetDir.normalize().startsWith(www.normalize()) || !Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
      System.err.println("[STATIC] Directory not found: " + requestPath + " -> " + targetDir);
      write(out, "404 Not Found", "text/plain", "Directory not found");
      return;
    }
    
    System.err.println("[LOCAL DEBUG] Directory listing requested for: " + targetDir);
    
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>\n");
    html.append("<html>\n<head>\n");
    html.append("<meta charset=\"UTF-8\">\n");
    html.append("<title>Directory Listing - LOCAL DEBUG ONLY</title>\n");
    html.append("</head>\n<body>\n");
    html.append("<h1 style='color:red;'>⚠️ LOCAL DEVELOPMENT ONLY</h1>\n");
    html.append("<p><strong>This feature is for local debugging only and should NEVER be used in production.</strong></p>\n");
    html.append("<h2>Directory: ").append(requestPath.isEmpty() ? "/" : requestPath).append("</h2>\n");
    html.append("<pre>\n");
    
    try {
      // Execute ls -al command on the target directory
      ProcessBuilder pb = new ProcessBuilder("ls", "-al", targetDir.toString());
      Process proc = pb.start();
      
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(proc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        // Escape HTML special characters
        line = line.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
        html.append(line).append("\n");
      }
      
      proc.waitFor();
      
    } catch (Exception e) {
      html.append("Error executing directory listing: ").append(e.getMessage()).append("\n");
    }
    
    html.append("</pre>\n");
    html.append("<p><a href='/'>← Back to home</a></p>\n");
    html.append("</body>\n</html>");
    
    write(out, "200 OK", "text/html; charset=UTF-8", html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
  
  String mime(String f) {
    if(f.endsWith(".html")) return "text/html; charset=UTF-8";
    if(f.endsWith(".js")) return "application/javascript; charset=UTF-8";
    if(f.endsWith(".css")) return "text/css; charset=UTF-8";
    if(f.endsWith(".png")) return "image/png";
    if(f.endsWith(".txt")) return "text/plain; charset=UTF-8";
    return "application/octet-stream";
  }
  
  void write(OutputStream o, String s, String c, byte[] b) throws IOException {
    o.write(("HTTP/1.1 " + s + "\r\nContent-Type: " + c + "\r\nContent-Length: " + b.length + "\r\n\r\n").getBytes());
    o.write(b);
  }
  
  void write(OutputStream o, String s, String c, String b) throws IOException {
    write(o, s, c, b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
}

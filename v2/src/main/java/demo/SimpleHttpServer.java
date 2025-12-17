
package demo;
import java.io.*; import java.net.*; import java.nio.file.*; import java.util.*;
public class SimpleHttpServer {
  public static void main(String[]a)throws Exception{
    Properties p=new Properties();
    p.load(SimpleHttpServer.class.getClassLoader().getResourceAsStream("application.properties"));
    int port=Integer.parseInt(p.getProperty("port","8080"));
    ServerSocket ss=new ServerSocket(port);
    Path out=Paths.get(p.getProperty("out.dir","./out"));
    StaticGetHandler gh=new StaticGetHandler(Paths.get(p.getProperty("www.dir","./www")), out);
    System.err.println("[SERVER] Started on port "+port);
    System.err.println("[SERVER] www.dir="+p.getProperty("www.dir","./www")+" out.dir="+out);
    while(true)try(Socket s=ss.accept()){
      ByteArrayOutputStream r=new ByteArrayOutputStream();
      InputStream in=s.getInputStream(); byte[] b=new byte[8192]; int n;
      
      // Read headers (until \r\n\r\n)
      while((n=in.read(b))!=-1){r.write(b,0,n);if(r.toString().contains("\r\n\r\n"))break;}
      
      // Check if there's a Content-Length header and read the body
      String headers=r.toString();
      int contentLength=extractContentLength(headers);
      if(contentLength>0){
        int headerEnd=headers.indexOf("\r\n\r\n")+4;
        int bodyBytesRead=r.size()-headerEnd;
        int remaining=contentLength-bodyBytesRead;
        
        // Read remaining body bytes
        while(remaining>0\u0026\u0026(n=in.read(b,0,Math.min(b.length,remaining)))!=-1){
          r.write(b,0,n);
          remaining-=n;
        }
      }
      
      String req=r.toString();
      String firstLine=req.split("\\r?\\n")[0];
      System.err.println("[REQUEST] "+firstLine);
      OutputStream o=s.getOutputStream();
      
      if(req.startsWith("GET")){
        // Save GET requests as .txt files
        String verb=RequestSaver.extractVerb(req);
        RequestSaver.saveText(verb,req,out);
        
        String path=req.split(" ")[1];
        if(path.startsWith(p.getProperty("handler1"))&&path.contains("?")){System.err.println("[ROUTE] Dynamic handler1: "+path);
          Map<String,String> m=new HashMap<>();
          for(String kv:path.split("[?]",2)[1].split("&")){
            String[] x=kv.split("="); m.put(x[0],x.length>1?x[1]:"");
          }
          new Extn().handle1(m,o); continue;
        }
        System.err.println("[ROUTE] Static GET: "+path);
        gh.handle(path,o);
      }else if(req.startsWith("POST")&&req.contains("multipart/form-data")){
        System.err.println("[ROUTE] Multipart POST");
        // Create folder and save request + attachments
        String folderName = createPostFolderName(out);
        Path folder = out.resolve(folderName);
        MultipartPostHandler.handle(req, folder);
        o.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes());
      }else if(req.startsWith("POST")){
        System.err.println("[ROUTE] Simple POST (form-urlencoded)");
        // Create folder and save request only
        String folderName = createPostFolderName(out);
        Path folder = out.resolve(folderName);
        createPostFolder(req, folder);
        o.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes());
      }else{
        o.write("HTTP/1.1 200 OK\r\n\r\nOK".getBytes());
      }
    }catch(Exception e){System.err.println("[ERROR] "+e.getMessage());e.printStackTrace();}
  }
  
  static String createPostFolderName(Path outDir) {
    String timestamp = java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        .replace(":", "-");
    return "POST_" + timestamp + "_" + new java.util.Random().nextInt(90);
  }
  
  static void createPostFolder(String req, Path folder) throws IOException {
    Files.createDirectories(folder);
    Files.writeString(folder.resolve("request.txt"), req);
    System.err.println("[POST FOLDER] Created: " + folder.getFileName());
  }
  
  static int extractContentLength(String headers) {
    for (String line : headers.split("\\r?\\n")) {
      if (line.toLowerCase().startsWith("content-length:")) {
        try {
          return Integer.parseInt(line.substring(15).trim());
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }
    return 0;
  }
}

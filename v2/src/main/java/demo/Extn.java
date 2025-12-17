
package demo;
import java.io.*; import java.util.*;
public class Extn {
  public void handle1(Map<String,String> p,OutputStream out)throws IOException{
    System.err.println("[HANDLER1] Params: "+p);
    try{
      double a1=Double.parseDouble(p.get("a1"));
      double a2=Double.parseDouble(p.get("a2"));
      double result=a1+a2;
      System.err.println("[HANDLER1] Result: "+a1+"+"+a2+"="+result);
      out.write(("HTTP/1.1 200 OK\r\nContent-Type:text/plain\r\n\r\n"+result).getBytes());
    }catch(Exception e){
      System.err.println("[HANDLER1] Error: "+e.getMessage());
      out.write(("HTTP/1.1 400 Bad Request\r\nContent-Type:text/plain\r\n\r\nerror").getBytes());
    }
  }
}

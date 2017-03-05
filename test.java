//import java.io.InputStreamReader;
import java.io.*;

import java.lang.Exception.*;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;

public class test {
  static int count;
  static BufferedWriter writer;

  public static void Signal(String s) {
    int n = 0;
    try {
      n = Integer.parseInt(s);
      System.out.println("signal:" + s);
    } catch (NumberFormatException e) {
      return ;
    }

    try {
      if (count > 9) {
        writer.write("-1");
        writer.newLine();
        writer.flush();
        return;
      }
      count++;
      writer.write("" + (n*2));
      writer.newLine();
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // static InputStream cout;
  public static void main(String[] args) {
    count = 0;
    try {
      ProcessBuilder pb = new ProcessBuilder("C:\\Users\\Yoshio\\Desktop\\Game\\othello\\1a.exe");
      Process process = pb.start();

      InputStreamThread it = new InputStreamThread(process.getInputStream(), true);
      InputStreamThread et = new InputStreamThread(process.getErrorStream());
      writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      it.prefix("cout");
      et.prefix("cerr");
      it.start();
      et.start();

      test.Signal("0");

      boolean end = false;
      try {
        end = process.waitFor(3, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        System.out.println(e.getClass() + "\t" + e.getMessage());
      }
      if (!end) {
        System.out.println("timeout");
        process.destroy();
      }
      it.join();
      et.join();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}

class InputStreamThread extends Thread {
  BufferedReader br;
  String p;
  boolean hasMgr;
  //test mgr;

  InputStreamThread(InputStream is) {
    br = new BufferedReader(new InputStreamReader(is));
    hasMgr = false;
  }
  // InputStreamThread(InputStream is, String charset) {
  //   try {
  //     br = new BufferedReader(new InputStreamReader(is, charset));
  //   } catch (UnsupportedEncodingException e) {
  //     throw new RuntimeException(e);
  //   }
  // }
  InputStreamThread(InputStream is, boolean i) {
    br = new BufferedReader(new InputStreamReader(is));
    hasMgr = true;
    //mgr = _mgr;
  }

  public void prefix(String str) {
    p = str;
  }

  @Override
  public void run() {
    try {
      for(;;) {
        String line = br.readLine();
        if (line == null) break;
        if (hasMgr) {
          //mgr.Signal(line);
          test.Signal(line);
        }
        System.out.println(p + ":" + line);
      }
    } catch (IOException e) {
        throw new RuntimeException(e);
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}

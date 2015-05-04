package org.hjson.test;

import org.hjson.*;
import java.io.*;
import static java.lang.System.out;

public class Main {

  private static String[] testNames={
    "comments_test.hjson",
    "fail10_test.json",
    "fail11_test.json",
    "fail12_test.json",
    "fail13_test.json",
    "fail14_test.json",
    "fail15_test.json",
    "fail16_test.json",
    "fail17_test.json",
    "fail19_test.json",
    "fail20_test.json",
    "fail21_test.json",
    "fail22_test.json",
    "fail23_test.json",
    "fail24_test.json",
    "fail26_test.json",
    "fail28_test.json",
    "fail29_test.json",
    "fail2_test.json",
    "fail30_test.json",
    "fail31_test.json",
    "fail32_test.json",
    "fail33_test.json",
    "fail5_test.json",
    "fail6_test.json",
    "fail7_test.json",
    "fail8_test.json",
    "failKey1_test.hjson",
    "failKey2_test.hjson",
    "failKey3_test.hjson",
    "failObj1_test.hjson",
    "failObj2_test.hjson",
    "failObj3_test.hjson",
    "kan_test.hjson",
    "keys_test.hjson",
    "oa_test.hjson",
    "pass1_test.json",
    "pass2_test.json",
    "pass3_test.json",
    "stringify1_test.hjson",
    "strings_test.hjson",
  };

  public static String convertStreamToString(InputStream is) throws IOException {
    Writer writer=new StringWriter();
    char[] buffer=new char[1024];
    try {
      Reader reader=new BufferedReader(new InputStreamReader(is, "UTF-8"));
      int n;
      while ((n=reader.read(buffer))!=-1) writer.write(buffer, 0, n);
    } finally {
      is.close();
    }
    return writer.toString();
  }

  private static String load(String file, boolean cr) throws Exception {
    InputStream res=Main.class.getResourceAsStream("/"+file);
    if (res==null) throw new Exception(file+" not found!");
    String text=convertStreamToString(res);
    String std=text.replace("\r", ""); // make sure we have unix style text regardless of the input
    return cr ? std.replace("\n", "\r\n") : std;
  }

  private static boolean test(String name, String file, boolean inputCr, boolean outputCr) throws Exception {
    int extIdx=file.lastIndexOf('.');
    boolean isJson=extIdx>=0 && file.substring(extIdx).equals(".json");
    boolean shouldFail=name.startsWith("fail");

    JsonValue.setEol(outputCr?"\r\n":"\n");
    String text=load(file, inputCr);

    try {
      JsonValue data=JsonValue.readHjson(text);
      String data1=data.toString(Stringify.FORMATTED);
      String hjson1=data.toString(Stringify.HJSON);
      if (!shouldFail) {
        JsonValue result=JsonValue.readJSON(load(name+"_result.json", inputCr));
        String data2=result.toString(Stringify.FORMATTED);
        String hjson2=load(name+"_result.hjson", outputCr);
        if (!data1.equals(data2)) return failErr(name, "parse", data1, data2);
        if (!hjson1.equals(hjson2)) return failErr(name, "stringify", hjson1, hjson2);

        if (isJson) {
          String json1=data.toString(), json2=JsonValue.readHjson(text).toString();
          if (!json1.equals(json2)) return failErr(name, "json chk", json1, json2);
        }
      }
      else return failErr(name, "should fail", null, null);
    }
    catch (Exception e) {
      if (!shouldFail) return failErr(name, "exception", e.toString(), "");
    }
    return true;
  }

  static boolean failErr(String name, String type, String s1, String s2) {
    out.println(name+" "+type+" FAILED!");
    if (s1!=null || s2!=null) {
      out.printf("--- actual (%d):\n", s1.length());
      out.println(s1+"---");
      out.printf("--- expected (%d):\n", s2.length());
      out.println(s2+"---");
      if (s1.length()==s2.length())
        for (int i=0; i<s1.length(); i++) {
          if (s1.charAt(i)!=s2.charAt(i)) {
            out.printf("Diff at offs %d: %d/%d\n", i, s1.charAt(i), s2.charAt(i));
            break;
          }
        }
    }
    return false;
  }

  public static void main(String[] args) throws Exception {

    out.println("running tests...");

    for (String file : testNames) {
      int extIdx=file.lastIndexOf('.');
      String name=file.substring(0, extIdx);
      name=name.substring(0, name.length()-5);
      //if (filter!=null && !name.Contains(filter)) continue;

      if (!test(name, file, false, false)
        || !test(name, file, true, false)
        || !test(name, file, false, true)
        || !test(name, file, true, true)) { System.exit(1); return; }
      out.println("- "+name+" OK");
    }
    out.println("ALL OK!");


  }
}

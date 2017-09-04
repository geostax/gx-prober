package com.geostax.prober.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandTool {

	public static void main(String[] args) {
		
		
		try {  
            Runtime rt = Runtime.getRuntime();  
            Process pr = rt.exec("cmd /c start nodetool.bat"); // cmd /c calc  
            // Process pr = rt.exec("D:\\xunlei\\project.aspx");  
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream(), "GBK"));  
  
            String line = null;  
  
            while ((line = input.readLine()) != null) {  
                System.out.println(line);  
            }  
  
            int exitVal = pr.waitFor();  
            System.out.println("Exited with error code " + exitVal);  
  
        } catch (Exception e) {  
            System.out.println(e.toString());  
            e.printStackTrace();  
        }  
		
		//String bat = "cmd /c start D:\\apache-cassandra-3.10\\bin\\cassandra.bat";
		
		
	    
		String cmd = "D:/apache-cassandra-3.10/bin/nodetool status";// pass
		//String cmd = "cmd /c start F:\\database_backup\\ngx_backup\\" + batName + ".bat";// pass
		try {
			Process ps = Runtime.getRuntime().exec(cmd);
			ps.waitFor();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("child thread done");
	}
}

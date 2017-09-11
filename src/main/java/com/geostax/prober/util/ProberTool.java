package com.geostax.prober.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.google.gson.Gson;

public class ProberTool {

	private String sigar;
	Gson gson=new Gson();
	
	public void setSigar(String sigar) {
		this.sigar = sigar;
	}
	

	public String getSysInfo() throws Exception {
		
		Map<String, String> items=null;
		Runtime runtime = Runtime.getRuntime();
		try {
			Process pr = runtime.exec(
					"cmd /c java -classpath .;%JAVA_HOME%\\lib;%JAVA_HOME%\\lib\\tools.jar;C:\\Users\\Phil\\Desktop\\gx-mbean.jar com.geostax.MBeanTest");
			BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream(), "GBK"));
			String line = null;
			while ((line = input.readLine()) != null) {
				items=gson.fromJson(line, Map.class);
				System.out.println(items);
			}
		} catch (Exception e) {
			System.out.println("Error!");
		}

		// Disk
		Map<String, Object> data = new HashMap<>();
		String path = System.getenv().get("CASSANDRA_HOME");
		File file = new File(path);
		DecimalFormat df = new DecimalFormat("#.00");

		// Cassandra
		data.put("ClientRequestWriteLatency", items.get("ClientRequestWriteLatency"));
		data.put("ClientRequestWriteFailures", items.get("ClientRequestWriteFailures"));
		data.put("ClientRequestWriteTimeouts", items.get("ClientRequestWriteTimeouts"));
		data.put("ClientRequestReadFailures", items.get("ClientRequestReadFailures"));
		data.put("ClientRequestReadLatency", items.get("ClientRequestReadLatency"));
		data.put("ClientRequestReadTimeouts", items.get("ClientRequestReadTimeouts"));
		return JSONUtil.toJson(data);
	}


	private double cpu(Sigar sigar) throws SigarException {
		CpuInfo infos[] = sigar.getCpuInfoList();
		CpuPerc cpuList[] = null;
		cpuList = sigar.getCpuPercList();
		double usage = 0.0;
		for (int i = 0; i < infos.length; i++) {// 不管是单块CPU还是多CPU都适用
			usage += cpuList[i].getCombined();
		}
		return usage / infos.length;
	}
	

	public static void main(String[] args) {
		args = new String[] { "10.88.40.185:9092", "monitor", "5000" };
		ProberTool tool = new ProberTool();

		try {
			String statusInfo = tool.getSysInfo();
			Map<String, Object> data = null;
			data = (Map<String, Object>) JSONUtil.fromJson(statusInfo, Map.class);
			System.out.println(data);
			Thread.sleep(Long.parseLong(args[2]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

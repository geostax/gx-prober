package com.geostax.prober.util;

import java.io.File;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ProberTool {

	private String sigar;
	private MBeanUtil mbean;
	
	public void setSigar(String sigar) {
		this.sigar = sigar;
	}
	
	public void setMbean(MBeanUtil mbean) {
		this.mbean = mbean;
	}

	public String getSysInfo() throws Exception {
		
		System.load(sigar);
		Sigar sigar = new Sigar();
		Map<String, String> items = mbean.getMBean();

		// Disk
		Map<String, Object> data = new HashMap<>();
		String path = System.getenv().get("CASSANDRA_HOME");
		File file = new File(path);
		DecimalFormat df = new DecimalFormat("#.00");
		data.put("disk_usage", df.format(Double.parseDouble(items.get("StorageLoad")) / 1024.0 / 1024 / 1024)); // GB
		data.put("disk_free", df.format(file.getFreeSpace() / 1024.0 / 1024 / 1024)); // GB
		
		// CPU
		double cpu_usage = cpu(sigar);
		data.put("cpu_usage", cpu_usage);
		data.put("cpu_free", 1 - cpu_usage);
		// Memory
		Mem mem = sigar.getMem();
		data.put("mem_total", df.format( mem.getTotal() / 1024.0 / 1024 / 1024));
		data.put("mem_used",  df.format(mem.getUsed() / 1024.0 / 1024 / 1024));
		data.put("mem_free",  df.format(mem.getFree() / 1024.0 / 1024 / 1024));

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

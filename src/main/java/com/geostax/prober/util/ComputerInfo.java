package com.geostax.prober.util;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ComputerInfo {

	public static ComputerInfo instance;
	
	private int cpu_num;
	private double ghz;
	private double disk;
	private double ram;
	private String os;
	private String network;
	
	
	
	public ComputerInfo() {
		// TODO Auto-generated constructor stub
	}

	public static ComputerInfo getInstance() {
		return instance;
	}

	public static void setInstance(ComputerInfo instance) {
		ComputerInfo.instance = instance;
	}

	public int getCpu_num() {
		return cpu_num;
	}

	public void setCpu_num(int cpu_num) {
		this.cpu_num = cpu_num;
	}

	public double getGhz() {
		return ghz;
	}

	public void setGhz(double ghz) {
		this.ghz = ghz;
	}

	public double getDisk() {
		return disk;
	}

	public void setDisk(double disk) {
		this.disk = disk;
	}

	public double getRam() {
		return ram;
	}

	public void setRam(double ram) {
		this.ram = ram;
	}
	
	
}

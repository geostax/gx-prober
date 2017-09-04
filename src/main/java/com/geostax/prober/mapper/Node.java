package com.geostax.prober.mapper;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "gx_prober", name = "node", caseSensitiveKeyspace = false, caseSensitiveTable = false)
public class Node {

	@PartitionKey
	private String ip;
	
	private String datacenter;
	private String rack;
	private int cpu_core;
	private String cpu_hz;
	private double disk;
	private String host_id;
	private String name;
	private double ram;
	private int tokens;

	public String getDatacenter() {
		return datacenter;
	}

	public void setDatacenter(String datacenter) {
		this.datacenter = datacenter;
	}

	public String getRack() {
		return rack;
	}

	public void setRack(String rack) {
		this.rack = rack;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getCpu_core() {
		return cpu_core;
	}

	public void setCpu_core(int cpu_core) {
		this.cpu_core = cpu_core;
	}

	public String getCpu_hz() {
		return cpu_hz;
	}

	public void setCpu_hz(String cpu_hz) {
		this.cpu_hz = cpu_hz;
	}

	public double getDisk() {
		return disk;
	}

	public void setDisk(double disk) {
		this.disk = disk;
	}

	public String getHost_id() {
		return host_id;
	}

	public void setHost_id(String host_id) {
		this.host_id = host_id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getRam() {
		return ram;
	}

	public void setRam(double ram) {
		this.ram = ram;
	}

	public int getTokens() {
		return tokens;
	}

	public void setTokens(int tokens) {
		this.tokens = tokens;
	}
}

package com.geostax.prober.mapper;

import java.util.Date;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

@Table(keyspace = "gx_prober", name = "current_status", caseSensitiveKeyspace = false, caseSensitiveTable = false)
public class Current {

	@PartitionKey
	String ip;
	Date time;
	String status;
	double cpu;
	double ram_usage;
	double ram_free;
	double disk_usage;
	double disk_free;
	double rl;
	double rf;
	double rt;
	double wl;
	double wf;
	double wt;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}

	public double getRam_usage() {
		return ram_usage;
	}

	public void setRam_usage(double ram_usage) {
		this.ram_usage = ram_usage;
	}

	public double getRam_free() {
		return ram_free;
	}

	public void setRam_free(double ram_free) {
		this.ram_free = ram_free;
	}

	public double getDisk_usage() {
		return disk_usage;
	}

	public void setDisk_usage(double disk_usage) {
		this.disk_usage = disk_usage;
	}

	public double getDisk_free() {
		return disk_free;
	}

	public void setDisk_free(double disk_free) {
		this.disk_free = disk_free;
	}

	public double getRl() {
		return rl;
	}

	public void setRl(double rl) {
		this.rl = rl;
	}

	public double getRf() {
		return rf;
	}

	public void setRf(double rf) {
		this.rf = rf;
	}

	public double getRt() {
		return rt;
	}

	public void setRt(double rt) {
		this.rt = rt;
	}

	public double getWl() {
		return wl;
	}

	public void setWl(double wl) {
		this.wl = wl;
	}

	public double getWf() {
		return wf;
	}

	public void setWf(double wf) {
		this.wf = wf;
	}

	public double getWt() {
		return wt;
	}

	public void setWt(double wt) {
		this.wt = wt;
	}

}

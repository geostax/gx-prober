package com.geostax.prober;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;

public class CassandraManager {
	private static final Logger logger = LoggerFactory.getLogger(CassandraManager.class);
	private String dc;
	private String rack;
	private String host;
	private String port;
	private String user;
	private String pwd;

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getHost() {
		return host;
	}

	public String getDc() {
		return dc;
	}

	public void setDc(String dc) {
		this.dc = dc;
	}

	public String getRack() {
		return rack;
	}

	public void setRack(String rack) {
		this.rack = rack;
	}

	Cluster cluster = null;
	Session session;

	public void init() {
		logger.info(">>>>> Cassandra initilized success, Host:{}, Port:{}", new Object[] { host, port });
		cluster = Cluster.builder().addContactPoints(host).build();
		this.session = cluster.connect();
	}

	public Session getSession() {
		if (session == null) {
			session = cluster.connect();
		}
		return session;
	}
	
	
	public Set<Host> getHosts(){
		return cluster.getMetadata().getAllHosts();
	}
	

	
	public void stop() {
		cluster.close();
	}
}

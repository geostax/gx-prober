package com.geostax.prober.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class MBeanTool {

	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final String commandname = "sun.java.command";

	String ProcessCpuLoad = "";
	long Memory = 0;
	String ThreadCount = "";
	private static final String C_WF = "ClientRequestWriteFailures";
	private static final String C_WL = "ClientRequestWriteLatency";
	private static final String C_WT = "ClientRequestWriteTimeouts";
	private static final String C_RF = "ClientRequestReadFailures";
	private static final String C_RL = "ClientRequestReadLatency";
	private static final String C_RT = "ClientRequestReadTimeouts";

	Map<String, Object> params = new HashMap<>();

	public Map<String, Object> getMbeanInfo() {

		Method attachToVM;
		List<VirtualMachineDescriptor> allvm;
		Method getVMId;
		Method loadAgent;
		Method getAgentProperties;
		Method getSystemProperties;
		Method detach;

		String javaHome = System.getProperty("java.home");
		String pathToAdd = javaHome + "\\jre\\bin\\attach.dll";

		try {
			URLClassLoader loader = new URLClassLoader(new URL[] { new File(pathToAdd).toURI().toURL() });
			Class virtualMachine = Class.forName("com.sun.tools.attach.VirtualMachine", true, loader);
			Class virtualMachineDescriptor = Class.forName("com.sun.tools.attach.VirtualMachineDescriptor", true,
					loader);
			Method getVMList = virtualMachine.getMethod("list", (Class[]) null);
			attachToVM = virtualMachine.getMethod("attach", String.class);
			getAgentProperties = virtualMachine.getMethod("getAgentProperties", (Class[]) null);
			getVMId = virtualMachineDescriptor.getMethod("id", (Class[]) null);
			allvm = VirtualMachine.list();
			getSystemProperties = virtualMachine.getMethod("getSystemProperties", (Class[]) null);
			loadAgent = virtualMachine.getMethod("loadAgent", String.class, String.class);
			detach = virtualMachine.getMethod("detach", (Class[]) null);
			for (Object vmInstance : allvm) {
				getVMId.invoke(VirtualMachineDescriptor.class.newInstance(), (Object[]) null);
				String id = (String) getVMId.invoke(vmInstance, (Object[]) null);
				Object vm = attachToVM.invoke(null, id);
				Properties systemProperties = (Properties) getSystemProperties.invoke(vm, (Object[]) null);
				String home = systemProperties.getProperty("java.home");
				String agent = home + File.separator + "jre" + File.separator + "lib" + File.separator
						+ "management-agent.jar";
				File f = new File(agent);
				if (!f.exists()) {
					agent = home + File.separator + "lib" + File.separator + "management-agent.jar";
					f = new File(agent);
					if (!f.exists()) {
						throw new IOException("Management agent not found");
					}
				}
				agent = f.getCanonicalPath();
				loadAgent.invoke(vm, agent, "com.sun.management.jmxremote");
				Properties agentProperties = (Properties) getAgentProperties.invoke(vm, (Object[]) null);
				String javaprocessname = agentProperties.getProperty(commandname);
				if (javaprocessname.equals("org.apache.cassandra.service.CassandraDaemon")) {
					String connectorAddress = agentProperties.getProperty(CONNECTOR_ADDRESS);
					if (connectorAddress == null) {
						continue;
					}
					JMXServiceURL url = new JMXServiceURL(connectorAddress);
					JMXConnector connector = JMXConnectorFactory.connect(url);
					MBeanServerConnection mbeanConn = connector.getMBeanServerConnection();
					Set<ObjectName> beanSet = mbeanConn.queryNames(null, null);
					Set MBeanset = mbeanConn.queryMBeans(null, null);
					Iterator MBeansetIterator = MBeanset.iterator();
					while (MBeansetIterator.hasNext()) {
						ObjectInstance objectInstance = (ObjectInstance) MBeansetIterator.next();
						ObjectName objectName = objectInstance.getObjectName();

						// ClientRequest Write
						if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Latency")) {
							params.put(C_WL, mbeanConn.getAttribute(objectName, "Count"));
						} else if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Timeouts")) {
							params.put(C_WT, mbeanConn.getAttribute(objectName, "Count"));

						} else if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Failures")) {
							params.put(C_WF, mbeanConn.getAttribute(objectName, "Count"));

						}

						// ClientRequest Read
						if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency")) {
							params.put(C_RL, mbeanConn.getAttribute(objectName, "Count"));

						} else if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Failures")) {
							params.put(C_RF, mbeanConn.getAttribute(objectName, "Count"));
						} else if (objectName.toString()
								.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Timeouts")) {
							params.put(C_RT, mbeanConn.getAttribute(objectName, "Count"));
						}
					}

					detach.invoke(vm, (Object[]) null);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return params;
	}

}

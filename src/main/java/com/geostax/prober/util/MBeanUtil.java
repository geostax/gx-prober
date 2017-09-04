package com.geostax.prober.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.MemoryUsage;
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
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class MBeanUtil {

	// private static getattributes gtt=new getattributes();
	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final String commandname = "sun.java.command";

	Map<String, String> params = new HashMap<>();

	String ProcessCpuLoad = "";
	long Memory = 0;
	String ThreadCount = "";
	String ClientRequestWriteFailures = "";
	String ClientRequestWriteLatency = "";
	String ClientRequestWriteTimeouts = "";
	String ClientRequestReadFailures = "";
	String ClientRequestReadLatency = "";
	String ClientRequestReadTimeouts = "";
	String StorageLoad = "";
	String StorageTotalHintsInProgress = "";

	Method attachToVM;
	List<VirtualMachineDescriptor> allvm;
	Method getVMId;
	Method loadAgent;
	Method getAgentProperties;
	Method getSystemProperties;
	Method detach;

	public MBeanUtil() {
		System.out.println("new MBeanUtil!!!");
		try {
			String javaHome ="C:\\Java\\jdk1.8.0_121";
			String tool_jar = "C:\\Java\\jdk1.8.0_121\\lib\\tools.jar";
			String pathToAdd = javaHome + "\\jre\\bin\\attach.dll";
			System.out.println(javaHome);
			URLClassLoader loader = new URLClassLoader(new URL[] { new File(pathToAdd).toURI().toURL(),new File(tool_jar).toURI().toURL() });
			
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
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Map<String, String> getMBean() throws Exception {
		for (Object vmInstance : allvm) {
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
					// Tasks
					if (objectName.toString().equals(
							"org.apache.cassandra.metrics:type=ThreadPools,path=transport,scope=Native-Transport-Requests,name=ActiveTasks")) {
						String ActiveTasks = mbeanConn.getAttribute(objectName, "Value").toString();

						params.put("ActiveTasks", ActiveTasks);
					} else if (objectName.toString().equals(
							"org.apache.cassandra.metrics:type=ThreadPools,path=transport,scope=Native-Transport-Requests,name=CompletedTasks")) {
						String CompletedTasks = mbeanConn.getAttribute(objectName, "Value").toString();
						params.put("CompletedTasks", CompletedTasks);
					} else if (objectName.toString().equals("java.lang:type=Threading")) {
						ThreadCount = mbeanConn.getAttribute(objectName, "ThreadCount").toString();
						params.put("ThreadCount", ThreadCount);
					} else if (objectName.toString().equals("java.lang:type=OperatingSystem")) {
						ProcessCpuLoad = mbeanConn.getAttribute(objectName, "ProcessCpuLoad").toString();
						params.put("ProcessCpuLoad", ProcessCpuLoad);
					} else if (objectName.toString().equals("java.lang:type=Memory")) {
						MemoryUsage heapMemoryUsage = MemoryUsage
								.from((CompositeData) mbeanConn.getAttribute(objectName, "HeapMemoryUsage"));
						Memory = heapMemoryUsage.getUsed();
						params.put("Memory", Memory + "");
					}
					// Byte
					else if (objectName.toString().equals("org.apache.cassandra.metrics:type=Storage,name=Load")) {
						StorageLoad = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("StorageLoad", StorageLoad);
					} else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=Storage,name=TotalHintsInProgress")) {
						StorageTotalHintsInProgress = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("StorageTotalHintsInProgress", StorageTotalHintsInProgress);
					}
					// ClientRequest Write
					else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Latency")) {
						ClientRequestWriteLatency = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestWriteLatency", ClientRequestWriteLatency);
					} else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Timeouts")) {
						ClientRequestWriteTimeouts = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestWriteTimeouts", ClientRequestWriteTimeouts);
					} else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Write,name=Failures")) {
						ClientRequestWriteFailures = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestWriteFailures", ClientRequestWriteFailures);
					}
					// ClientRequest Read
					else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Latency")) {
						ClientRequestReadLatency = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestReadLatency", ClientRequestReadLatency);
					} else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Failures")) {
						ClientRequestReadFailures = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestReadFailures", ClientRequestReadFailures);
					} else if (objectName.toString()
							.equals("org.apache.cassandra.metrics:type=ClientRequest,scope=Read,name=Timeouts")) {
						ClientRequestReadTimeouts = mbeanConn.getAttribute(objectName, "Count").toString();
						params.put("ClientRequestReadTimeouts", ClientRequestReadTimeouts);
					}
				}
				detach.invoke(vm, (Object[]) null);
			}
		}
		return params;
	}

	public static void main(String[] args) throws Exception {
		Map<String, String> bean = new MBeanUtil().getMBean();
		System.out.println(bean);

	}
}

package com.geostax.prober.util;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.Swap;

public class SigarTool {

	public final static String CPU_CORE_NUM="cpu_core_num";
	public final static String CPU_CORE_GHZ="cpu_core_ghz";
	public final static String CPU_USAGE="cpu_usage";
	public final static String CPU_FREE="cpu_free";
	public final static String CPU_USAGE_PER="cpu_usage_per";
	
	public final static String RAM_USAGE="ram_usage";
	public final static String RAM_FREE="ram_free";
	public final static String RAM_TOTAL="ram_total";
	
	public final static String DISK_USAGE="disk_usage";
	public final static String DISK_FREE="disk_free";
	public final static String DISK_TOTAL="disk_total";
	
	public final static String OS_NAME="os_name";
	public final static String OS_VERSION="os_version";
	
	public final static String NET_NAME="net_name";
	public final static String NET_ADDRESS="net_address";
	public final static String NET_RX_PACK="net_RxPackets";
	public final static String NET_TX_PACK="net_TxPackets";
	public final static String NET_RX_BYTES="net_RxBytes";
	public final static String NET_TX_BYTES="net_TxBytes";
	public final static String NET_RX_ERRORS="net_RxErrors";
	public final static String NET_TX_ERRORS="net_TxErrors";
	public final static String NET_RX_DROP="net_RxDropped";
	public final static String NET_TX_DROP="net_TxDropped";
	
	//"E:\\项目备份\\监控\\gx-prober\\lib\\sigar-amd64-winnt.dll"
	private String dll;
	public void setDll(String dll) {
		this.dll = dll;
	}
	public Map<String,Object> getSystemInfo(){
		System.load(dll);
		Sigar sigar = new Sigar();
		Map<String,Object> sys_info=new HashMap<>();
		try {
			long[] pids = sigar.getProcList();
			double total = 0;
			
			/**
			 * ----------------CPU-----------------
			 * */
			int cpuLength = sigar.getCpuInfoList().length;
			sys_info.put(CPU_CORE_NUM, cpuLength);
			
			CpuInfo infos[] = sigar.getCpuInfoList();
			for (int i = 0; i < infos.length; i++) {// 不管是单块CPU还是多CPU都适用
				CpuInfo info = infos[i];
				sys_info.put(CPU_CORE_GHZ, info.getMhz()/1000.0);// CPU的总量MHz
				break;
			}
						
			CpuPerc perc = sigar.getCpuPerc();
			// 获取当前cpu的占用率	
			BigDecimal bigDecimal =new BigDecimal(perc.getIdle());  
			sys_info.put(CPU_USAGE,  bigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());

			// 获取当前cpu的空闲率
			bigDecimal =new BigDecimal(perc.getCombined());  
			sys_info.put(CPU_USAGE,  bigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());
			
			//每个cpu的资源占用情况
			CpuPerc[] cpuPercs = sigar.getCpuPercList();
			Map<String,Double> cpu_usage_per=new HashMap<>();
			int count=1;
			for (CpuPerc cpuPerc : cpuPercs) {
				bigDecimal =new BigDecimal(cpuPerc.getCombined());  
				cpu_usage_per.put("core "+count, bigDecimal.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());
				count++;
			}
			sys_info.put(CPU_USAGE_PER, cpu_usage_per);
			
			/**
			 * ----------------RAM-----------------
			 * */
			Mem mem = sigar.getMem();
			sys_info.put(RAM_TOTAL,  new BigDecimal(mem.getTotal() / 1024.0 / 1024L/1024L).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());// 内存总量
			sys_info.put(RAM_USAGE,  new BigDecimal(mem.getUsed() / 1024.0 / 1024L/1024L).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());// 内存总量
			sys_info.put(RAM_FREE,  new BigDecimal(mem.getFree() / 1024.0 / 1024L/1024L).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue());// 内存总量
			
			/**
			 * ----------------DISK-----------------
			 * */
			
			FileSystem fslist[] = sigar.getFileSystemList();
			String dir = System.getenv().get("CASSANDRA_HOME");
			FileSystemUsage fsu= sigar.getFileSystemUsage(dir);
			sys_info.put(DISK_USAGE,new BigDecimal(fsu.getUsed()/1024.0/1024.0).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue()); // GB
			sys_info.put(DISK_FREE, new BigDecimal(fsu.getFree()/1024.0/1024).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue()); // GB.
			sys_info.put(DISK_TOTAL, new BigDecimal(fsu.getTotal()/1024.0/1024).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue()); // GB.
			
			/**
			 * ----------------OS-----------------
			 * */
			OperatingSystem OS = OperatingSystem.getInstance();			// 取当前操作系统的信息
			sys_info.put(OS_NAME, OS.getVendorName());
			sys_info.put(OS_VERSION,  OS.getDataModel());

			/**
			 * ----------------Network-----------------
			 * */
				
			String ip=InetAddress.getLocalHost().getHostAddress();
			// 获取网络流量等信息
			String ifNames[] = sigar.getNetInterfaceList();
			for (int i = 0; i < ifNames.length; i++) {
				String name = ifNames[i];
				NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
				if(ip.equals(ifconfig.getAddress())){
					sys_info.put(NET_NAME, name);
					sys_info.put(NET_ADDRESS, ip);
					NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
					sys_info.put(NET_RX_PACK,ifstat.getRxPackets()); // GB
					sys_info.put(NET_TX_PACK,ifstat.getTxPackets()); // GB
					sys_info.put(NET_RX_BYTES,ifstat.getRxBytes()); // GB
					sys_info.put(NET_TX_BYTES,ifstat.getTxBytes()); // GB
					sys_info.put(NET_RX_ERRORS,ifstat.getRxErrors()); // GB
					sys_info.put(NET_TX_ERRORS,ifstat.getTxErrors()); // GB
					sys_info.put(NET_RX_DROP,ifstat.getRxDropped()); // GB
					sys_info.put(NET_TX_DROP,ifstat.getTxDropped()); // GB
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return sys_info;
	}
	public static void main(String[] args) throws Exception {
		
		Sigar sigar = new Sigar();

		try {
			long[] pids = sigar.getProcList();
			double total = 0;
			 
		

			System.out.println("=======================CPU=========================");

			// CPU数量（单位：个）
			int cpuLength = sigar.getCpuInfoList().length;
			System.out.println("core=" +cpuLength);

			// CPU的总量（单位：HZ）及CPU的相关信息
			CpuInfo infos[] = sigar.getCpuInfoList();
			for (int i = 0; i < infos.length; i++) {// 不管是单块CPU还是多CPU都适用
				CpuInfo info = infos[i];
				System.out.println("mhz=" + info.getMhz());// CPU的总量MHz
				System.out.println("vendor=" + info.getVendor());// 获得CPU的卖主，如：Intel
				System.out.println("model=" + info.getModel());// 获得CPU的类别，如：Celeron
				System.out.println("cache size=" + info.getCacheSize());// 缓冲存储器数量
				break;
			}

			System.out.println();

			CpuPerc perc = sigar.getCpuPerc();
			System.out.println("整体cpu的占用情况:");
			System.out.println("system idle: " + perc.getIdle());// 获取当前cpu的空闲率
			System.out.println("conbined: " + perc.getCombined());// 获取当前cpu的占用率
			System.out.println();
			CpuPerc[] cpuPercs = sigar.getCpuPercList();
			System.out.println("每个cpu的资源占用情况:");
			for (CpuPerc cpuPerc : cpuPercs) {
				System.out.println("system idle: " + cpuPerc.getIdle());// 获取当前cpu的空闲率
				System.out.println("conbined: " + cpuPerc.getCombined());// 获取当前cpu的占用率
				System.out.println();
			}

			System.out.println("=======================Mem=========================");
			// 物理内存信息
			Mem mem = sigar.getMem();
			// 内存总量
			System.out.println("Total = " + mem.getTotal()/ 1024.0  / 1024L / 1024 + "M av");
			// 当前内存使用量
			System.out.println("Used = " + mem.getUsed()/ 1024.0  / 1024L / 1024 + "M used");
			// 当前内存剩余量
			System.out.println("Free = " + mem.getFree() / 1024.0  / 1024L / 1024 + "M free");

			// 系统页面文件交换区信息
			Swap swap = sigar.getSwap();
			// 交换区总量
			System.out.println("Total = " + swap.getTotal() / 1024L + "K av");
			// 当前交换区使用量
			System.out.println("Used = " + swap.getUsed() / 1024L + "K used");
			// 当前交换区剩余量
			System.out.println("Free = " + swap.getFree() / 1024L + "K free");
			System.out.println();

			System.out.println("=======================Disk=========================");
			FileSystem fslist[] = sigar.getFileSystemList();
			String dir = System.getProperty("user.home");// 当前用户文件夹路径
			System.out.println(dir + "   " + fslist.length);
			
			FileSystemUsage fsu= sigar.getFileSystemUsage("E:\\apache-cassandra-3.10");
			System.out.println(">>>"+fsu.getFree()/1024/1024);
			System.out.println(">>>"+fsu.getUsed()/1024/1024);
			System.out.println(">>>"+fsu.getUsePercent());
			 
			for (int i = 0; i < fslist.length; i++) {
				System.out.println("\n~~~~~~~~~~" + i + "~~~~~~~~~~");
				FileSystem fs = fslist[i];
				// 分区的盘符名称
				System.out.println("fs.getDevName() = " + fs.getDevName());
				// 分区的盘符名称
				System.out.println("fs.getDirName() = " + fs.getDirName());
				System.out.println("fs.getFlags() = " + fs.getFlags());//
				// 文件系统类型，比如 FAT32、NTFS
				System.out.println("fs.getSysTypeName() = " + fs.getSysTypeName());
				// 文件系统类型名，比如本地硬盘、光驱、网络文件系统等
				System.out.println("fs.getTypeName() = " + fs.getTypeName());
				// 文件系统类型
				System.out.println("fs.getType() = " + fs.getType());
				FileSystemUsage usage = null;
				try {
					usage = sigar.getFileSystemUsage(fs.getDirName());
				} catch (SigarException e) {
					if (fs.getType() == 2)
						throw e;
					continue;
				}
				switch (fs.getType()) {
				case 0: // TYPE_UNKNOWN ：未知
					break;
				case 1: // TYPE_NONE
					break;
				case 2: // TYPE_LOCAL_DISK : 本地硬盘
					// 文件系统总大小
					System.out.println(" Total = " + usage.getTotal()/1024/1024 + "KB");
					// 文件系统剩余大小
					System.out.println(" Free = " + usage.getFree()/1024/1024 + "KB");
					// 文件系统可用大小
					System.out.println(" Avail = " + usage.getAvail()/1024/1024 + "KB");
					// 文件系统已经使用量
					System.out.println(" Used = " + usage.getUsed()/1024/1024 + "KB");
					double usePercent = usage.getUsePercent() * 100D;
					// 文件系统资源的利用率
					System.out.println(" Usage = " + usePercent + "%");
					break;
				case 3:// TYPE_NETWORK ：网络
					break;
				case 4:// TYPE_RAM_DISK ：闪存
					break;
				case 5:// TYPE_CDROM ：光驱
					break;
				case 6:// TYPE_SWAP ：页面交换
					break;
				}
				System.out.println(" DiskReads = " + usage.getDiskReads());
				System.out.println(" DiskWrites = " + usage.getDiskWrites());
			}

			System.out.println("=======================OS=========================");
			// 取当前操作系统的信息
			OperatingSystem OS = OperatingSystem.getInstance();
			// 操作系统内核类型如： 386、486、586等x86
			System.out.println("OS.getArch() = " + OS.getArch());
			System.out.println("OS.getCpuEndian() = " + OS.getCpuEndian());//
			System.out.println("OS.getDataModel() = " + OS.getDataModel());//
			// 系统描述
			System.out.println("OS.getDescription() = " + OS.getDescription());
			System.out.println("OS.getMachine() = " + OS.getMachine());//
			// 操作系统类型
			System.out.println("OS.getName() = " + OS.getName());
			System.out.println("OS.getPatchLevel() = " + OS.getPatchLevel());//
			// 操作系统的卖主
			System.out.println("OS.getVendor() = " + OS.getVendor());
			// 卖主名称
			System.out.println("OS.getVendorCodeName() = " + OS.getVendorCodeName());
			// 操作系统名称
			System.out.println("OS.getVendorName() = " + OS.getVendorName());
			// 操作系统卖主类型
			System.out.println("OS.getVendorVersion() = " + OS.getVendorVersion());
			// 操作系统的版本号
			System.out.println("OS.getVersion() = " + OS.getVersion());
			System.out.println();

			System.out.println("=======================Network=========================");
			
			
			try {
				System.out.println(InetAddress.getLocalHost().getCanonicalHostName());
			} catch (UnknownHostException e) {
				try {
					System.out.println(sigar.getFQDN());
				} catch (SigarException ex) {
				} finally {
					sigar.close();
				}
			}

			// 取到当前机器的IP地址
			String address = null;
			
			try {
				address = InetAddress.getLocalHost().getHostAddress();
				// 没有出现异常而正常当取到的IP时，如果取到的不是网卡循回地址时就返回
				// 否则再通过Sigar工具包中的方法来获取
				System.out.println(address);
				if (!NetFlags.LOOPBACK_ADDRESS.equals(address)) {
				}
			} catch (UnknownHostException e) {
				// hostname not in DNS or /etc/hosts
			}
			try {
				address = sigar.getNetInterfaceConfig().getAddress();
			} catch (SigarException e) {
				address = NetFlags.LOOPBACK_ADDRESS;
			} finally {
			}

		
			String ip=InetAddress.getLocalHost().getHostAddress();
			// 获取网络流量等信息
			String ifNames[] = sigar.getNetInterfaceList();
			for (int i = 0; i < ifNames.length; i++) {
				String name = ifNames[i];
				
				NetInterfaceConfig ifconfig = sigar.getNetInterfaceConfig(name);
				if(!ip.equals(ifconfig.getAddress())){
					continue;
				}
				System.out.println("\nname = " + name);// 网络设备名
				System.out.println("Address = " + address);// IP地址
				System.out.println("Address = " + ifconfig.getAddress());// IP地址
				System.out.println("Netmask = " + ifconfig.getNetmask());// 子网掩码
				
				if ((ifconfig.getFlags() & 1L) <= 0L) {
					System.out.println("!IFF_UP...skipping getNetInterfaceStat");
					continue;
				}
				try {
					NetInterfaceStat ifstat = sigar.getNetInterfaceStat(name);
					System.out.println("RxPackets = " + ifstat.getRxPackets());// 接收的总包裹数
					System.out.println("TxPackets = " + ifstat.getTxPackets());// 发送的总包裹数
					System.out.println("RxBytes = " + ifstat.getRxBytes());// 接收到的总字节数
					System.out.println("TxBytes = " + ifstat.getTxBytes());// 发送的总字节数
					System.out.println("RxErrors = " + ifstat.getRxErrors());// 接收到的错误包数
					System.out.println("TxErrors = " + ifstat.getTxErrors());// 发送数据包时的错误数
					System.out.println("RxDropped = " + ifstat.getRxDropped());// 接收时丢弃的包数
					System.out.println("TxDropped = " + ifstat.getTxDropped());// 发送时丢弃的包数
					System.out.println();
				} catch (SigarNotImplementedException e) {
				} catch (SigarException e) {
					System.out.println(e.getMessage());
				}
			}
		} catch (SigarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package com.geostax.prober;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.geostax.prober.bean.StatusAddJobBean;
import com.geostax.prober.bean.StatusCollectJobBean;
import com.geostax.prober.biz.ExecutorBiz;
import com.geostax.prober.biz.impl.ExecutorBizImpl;
import com.geostax.prober.dao.ProberDao;
import com.geostax.prober.handler.IJobHandler;
import com.geostax.prober.handler.JobHander;
import com.geostax.prober.mapper.Node;
import com.geostax.prober.rpc.netcom.NetComServerFactory;
import com.geostax.prober.thread.JobThread;
import com.geostax.prober.util.AdminApiUtil;
import com.geostax.prober.util.MBeanTool;
import com.geostax.prober.util.ProberTool;
import com.geostax.prober.util.SigarTool;

public class GXProber implements ApplicationContextAware, ApplicationListener {

	private static final Logger logger = LoggerFactory.getLogger(GXProber.class);

	private String ip;
	private int port = 9999;
	private String appName;
	private String adminAddresses;
	public static String logPath;

	public static String address;
	public static SigarTool sigarTool;
	public static ProberTool proberTool;
	public static MBeanTool mbeanTool;
	public static ProberDao proberDao;
	public static CassandraManager cassandraManager;

	// Scheduler
	private static Scheduler scheduler;

	public void setScheduler(Scheduler scheduler) {
		GXProber.scheduler = scheduler;
		try {
			
			//removeJob("1", "1");
			//removeJob("2", "1");
			//Collect status of this node every 10 sec
			addJob(StatusCollectJobBean.class,"1", "1", "*/10 * * * * ?");
			//Write status to storage every 1 min
			addJob(StatusAddJobBean.class,"2", "1", "0 */1 * * * ?");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void setCassandraManager(CassandraManager cassandraManager) {
		GXProber.cassandraManager = cassandraManager;
	}

	public GXProber() {

	}

	public void setIp(String ip) {
		this.ip = ip;
		address = ip;
	}

	public String getIP() {
		return ip;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setAdminAddresses(String adminAddresses) {
		this.adminAddresses = adminAddresses;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	/**
	 * Create Jetty Server
	 */
	private NetComServerFactory serverFactory = new NetComServerFactory();

	public void start() throws Exception {
		// admin api util init
		AdminApiUtil.init(adminAddresses);
		// executor start
		NetComServerFactory.putService(ExecutorBiz.class, new ExecutorBizImpl());
		// Start server
		serverFactory.start(port, ip, appName);

	}

	public void destroy() {
		// 1、executor registry thread stop
		// ExecutorRegistryThread.getInstance().toStop();

		// 2、executor stop
		serverFactory.destroy();

		// 3、job thread repository destory
		if (JobThreadRepository.size() > 0) {
			for (Map.Entry<Integer, JobThread> item : JobThreadRepository.entrySet()) {
				JobThread jobThread = item.getValue();
				jobThread.toStop("Web容器销毁终止");
				jobThread.interrupt();

			}
			JobThreadRepository.clear();
		}

		// 4、trigger callback thread stop
		// TriggerCallbackThread.getInstance().toStop();
	}

	/**
	 * init job handler
	 * 
	 */
	public static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		GXProber.applicationContext = applicationContext;
		GXProber.proberDao = applicationContext.getBean(ProberDao.class);
		GXProber.proberTool = (ProberTool) applicationContext.getBean("proberTool");
		GXProber.sigarTool=(SigarTool)applicationContext.getBean("sigarTool");
		GXProber.mbeanTool=(MBeanTool)applicationContext.getBean("mbeanTool");
		
		// Register this node
		Node node = new Node();
		try {
			//node.setIp(InetAddress.getLocalHost().getHostAddress());
			Map<String, Object> sys_info=sigarTool.getSystemInfo();
			node.setIp(sys_info.get(SigarTool.NET_ADDRESS).toString());
			node.setCpu_core((int)sys_info.get(SigarTool.CPU_CORE_NUM));
			node.setCpu_hz(sys_info.get(SigarTool.CPU_CORE_GHZ).toString());
			node.setDatacenter(cassandraManager.getDc());
			node.setRack(cassandraManager.getRack());
			node.setRam((double)sys_info.get(SigarTool.RAM_TOTAL));
			node.setDisk((double)sys_info.get(SigarTool.DISK_TOTAL));
			node.setHost_id(sys_info.get(SigarTool.NET_ADDRESS).toString());
		
			proberDao.register(node);
			proberDao.addLog(new Date(), "正常",
					"新节点 (datacenter1-rack1-" + InetAddress.getLocalHost().getHostAddress() + ") 加入", "");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// init job handler action
		Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(JobHander.class);

		if (serviceBeanMap != null && serviceBeanMap.size() > 0) {
			for (Object serviceBean : serviceBeanMap.values()) {
				if (serviceBean instanceof IJobHandler) {
					String name = serviceBean.getClass().getAnnotation(JobHander.class).value();
					IJobHandler handler = (IJobHandler) serviceBean;
					registJobHandler(name, handler);
				}
			}
		}
	}

	/**
	 * job handler repository
	 * 
	 */
	private static ConcurrentHashMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<String, IJobHandler>();

	public static IJobHandler registJobHandler(String name, IJobHandler jobHandler) {
		logger.info(">>>>> GX-Prober register Handler success, name:{}, jobHandler:{}", name, jobHandler);
		proberDao.addLog(new Date(), "正常",
				"GX-Prober 注册新的handler, 名称:{" + name + "}, jobHandler:{" + jobHandler.getClass().getSimpleName() + "}",
				"");
		return jobHandlerRepository.put(name, jobHandler);
	}

	/**
	 * job Thread repository
	 * 
	 */
	private static ConcurrentHashMap<Integer, JobThread> JobThreadRepository = new ConcurrentHashMap<Integer, JobThread>();

	public static JobThread registJobThread(int jobId, IJobHandler handler, String removeOldReason) {
		JobThread newJobThread = new JobThread(handler);
		newJobThread.start();
		logger.info(">>>>> GX-Prober regist Thread success, jobId:{}, handler:{}", new Object[] { jobId, handler });

		JobThread oldJobThread = JobThreadRepository.put(jobId, newJobThread);
		if (oldJobThread != null) {
			oldJobThread.toStop(removeOldReason);
			oldJobThread.interrupt();
		}

		return newJobThread;
	}

	public static IJobHandler loadJobHandler(String name) {
		return jobHandlerRepository.get(name);
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {

	}

	public static JobThread loadJobThread(int jobId) {
		JobThread jobThread = JobThreadRepository.get(jobId);
		return jobThread;
	}

	public static void removeJobThread(int jobId, String removeOldReason) {
		JobThread oldJobThread = JobThreadRepository.remove(jobId);
		if (oldJobThread != null) {
			oldJobThread.toStop(removeOldReason);
			oldJobThread.interrupt();
		}
	}

	/**
	 * Schedule Job Operation
	 * 
	 * (Add, Reschedule, Remove, Pause, Resume)
	 * 
	 * /
	 * 
	 * /** addJob 新增
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param cronExpression
	 * @return
	 * @throws SchedulerException
	 */
	@SuppressWarnings("unchecked")
	public static boolean addJob(Class<? extends Job>  clazz,String jobName, String jobGroup, String cronExpression) throws SchedulerException {
		// TriggerKey : name + group
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
		JobKey jobKey = new JobKey(jobName, jobGroup);

		// TriggerKey valid if_exists
		if (checkExists(jobName, jobGroup)) {
			logger.info(">>>>>>>>> addJob fail, job already exist, jobGroup:{}, jobName:{}", jobGroup, jobName);
			return false;
		}

		// CronTrigger : TriggerKey + cronExpression
		// withMisfireHandlingInstructionDoNothing 忽略掉调度终止过程中忽略的调度
		CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
				.withMisfireHandlingInstructionDoNothing();
		CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder)
				.build();

		// JobDetail : jobClass
		// Class<? extends Job> jobClass_ = LocalJobBean.class;
		//Class<? extends Job> jobClass_ = AddStatusJobBean.class;
		Class<? extends Job> jobClass_ =clazz;
		JobDetail jobDetail = JobBuilder.newJob(jobClass_).withIdentity(jobKey).build();

		// schedule : jobDetail + cronTrigger
		Date date = scheduler.scheduleJob(jobDetail, cronTrigger);

		logger.info(">>>>>>>>>>> addJob success, jobDetail:{}, cronTrigger:{}, date:{}", jobDetail, cronTrigger, date);
		System.out.println(proberDao);
		proberDao.addLog(new Date(), "正常", "新建调度任务：" + jobDetail.getDescription(), "");

		return true;
	}

	/**
	 * reschedule
	 * 
	 * @param jobGroup
	 * @param jobName
	 * @param cronExpression
	 * @return
	 * @throws SchedulerException
	 */
	public static boolean rescheduleJob(String jobGroup, String jobName, String cronExpression)
			throws SchedulerException {

		// TriggerKey valid if_exists
		if (!checkExists(jobName, jobGroup)) {
			logger.info(">>>>>>>>>>> rescheduleJob fail, job not exists, JobGroup:{}, JobName:{}", jobGroup, jobName);
			return false;
		}

		// TriggerKey : name + group
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
		CronTrigger oldTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);

		if (oldTrigger != null) {
			// avoid repeat
			String oldCron = oldTrigger.getCronExpression();
			if (oldCron.equals(cronExpression)) {
				return true;
			}

			// CronTrigger : TriggerKey + cronExpression
			CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
					.withMisfireHandlingInstructionDoNothing();
			oldTrigger = oldTrigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder)
					.build();

			// rescheduleJob
			scheduler.rescheduleJob(triggerKey, oldTrigger);
		} else {
			// CronTrigger : TriggerKey + cronExpression
			CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression)
					.withMisfireHandlingInstructionDoNothing();
			CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey)
					.withSchedule(cronScheduleBuilder).build();

			// JobDetail-JobDataMap fresh
			JobKey jobKey = new JobKey(jobName, jobGroup);
			JobDetail jobDetail = scheduler.getJobDetail(jobKey);
			/*
			 * JobDataMap jobDataMap = jobDetail.getJobDataMap();
			 * jobDataMap.clear();
			 * jobDataMap.putAll(JacksonUtil.readValue(jobInfo.getJobData(),
			 * Map.class));
			 */

			// Trigger fresh
			HashSet<Trigger> triggerSet = new HashSet<Trigger>();
			triggerSet.add(cronTrigger);

			scheduler.scheduleJob(jobDetail, triggerSet, true);
		}

		logger.info(">>>>>>>>>>> resumeJob success, JobGroup:{}, JobName:{}", jobGroup, jobName);
		return true;
	}

	// unscheduleJob
	public static boolean removeJob(String jobName, String jobGroup) throws SchedulerException {
		// TriggerKey : name + group
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
		boolean result = false;
		if (checkExists(jobName, jobGroup)) {
			result = scheduler.unscheduleJob(triggerKey);
			logger.info(">>>>>>>>>>> removeJob, triggerKey:{}, result [{}]", triggerKey, result);
		}
		return true;
	}

	// Pause
	public static boolean pauseJob(String jobName, String jobGroup) throws SchedulerException {
		// TriggerKey : name + group
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);

		boolean result = false;
		if (checkExists(jobName, jobGroup)) {
			scheduler.pauseTrigger(triggerKey);
			result = true;
			logger.info(">>>>>>>>>>> pauseJob success, triggerKey:{}", triggerKey);
		} else {
			logger.info(">>>>>>>>>>> pauseJob fail, triggerKey:{}", triggerKey);
		}
		return result;
	}

	// resume
	public static boolean resumeJob(String jobName, String jobGroup) throws SchedulerException {
		// TriggerKey : name + group
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);

		boolean result = false;
		if (checkExists(jobName, jobGroup)) {
			scheduler.resumeTrigger(triggerKey);
			result = true;
			logger.info(">>>>>>>>>>> resumeJob success, triggerKey:{}", triggerKey);
		} else {
			logger.info(">>>>>>>>>>> resumeJob fail, triggerKey:{}", triggerKey);
		}
		return result;
	}

	// check if exists
	public static boolean checkExists(String jobName, String jobGroup) throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
		return scheduler.checkExists(triggerKey);
	}

}

package com.geostax.prober.bean;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.datastax.driver.core.Host;
import com.geostax.prober.CassandraManager;
import com.geostax.prober.GXProber;
import com.geostax.prober.mapper.Current;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.model.TriggerParam;
import com.geostax.prober.util.JSONUtil;

public class StatusCollectJobBean extends QuartzJobBean {

	private static Logger logger = LoggerFactory.getLogger(StatusCollectJobBean.class);

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

		// load job
		JobKey jobKey = context.getTrigger().getJobKey();
		Integer jobId = Integer.valueOf(jobKey.getName());
		TriggerParam triggerParam = new TriggerParam();

		// do trigger
		ReturnT<String> triggerResult = doTrigger(triggerParam);

	}

	public ReturnT<String> doTrigger(TriggerParam triggerParam) {
		ReturnT<String> result = runExecutor(triggerParam);
		System.out.println(">>>>> " + result);

		try {
			String statusInfo = GXProber.proberTool.getSysInfo();
			Map<String, Object> data = null;
			data = (Map<String, Object>) JSONUtil.fromJson(statusInfo, Map.class);
			Map<String, Object> sysinfo=GXProber.sigarTool.getSystemInfo();
			Current current = new Current();
			Date now = new Date();
			current.setIp(GXProber.address);
			current.setTime(now);
			current.setStatus(getState());
			current.setCpu((double)sysinfo.get("cpu_usage"));
			current.setDisk_free((double)sysinfo.get("disk_free"));
			current.setDisk_usage((double)sysinfo.get("disk_usage"));
			current.setRam_free((double)sysinfo.get("ram_free"));
			current.setRam_usage((double)sysinfo.get("ram_usage"));
			current.setWf(Double.parseDouble(data.get("ClientRequestWriteFailures").toString()));
			current.setWl(Double.parseDouble(data.get("ClientRequestWriteLatency").toString()));
			current.setWt(Double.parseDouble(data.get("ClientRequestWriteTimeouts").toString()));
			current.setRf(Double.parseDouble(data.get("ClientRequestReadFailures").toString()));
			current.setRl(Double.parseDouble(data.get("ClientRequestReadLatency").toString()));
			current.setRt(Double.parseDouble(data.get("ClientRequestReadTimeouts").toString()));
			GXProber.proberDao.setCurrent(current);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}
	
	public String getState(){
		CassandraManager cm=GXProber.cassandraManager;
		Set<Host> host_set= cm.getHosts();
		for(Host host:host_set){
			if(host.getAddress().equals(GXProber.address)){
				return host.getState();
			}
		}
		return null;
		
	}

	/**
	 * run executor
	 * 
	 * @param triggerParam
	 * @param address
	 * @return
	 */
	public ReturnT<String> runExecutor(TriggerParam triggerParam) {
		ReturnT<String> runResult = null;
		runResult = new ReturnT<>();
		runResult.setMsg("Lalalalala");
		runResult.setCode(ReturnT.SUCCESS_CODE);
		StringBuffer sb = new StringBuffer("触发调度：");
		sb.append("<br>code：").append(runResult.getCode());
		sb.append("<br>msg：").append(runResult.getMsg());
		runResult.setMsg(sb.toString());

		return runResult;
	}
}

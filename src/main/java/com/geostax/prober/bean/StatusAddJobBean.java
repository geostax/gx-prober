package com.geostax.prober.bean;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import com.geostax.prober.mapper.Status;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.model.TriggerParam;
import com.geostax.prober.util.JSONUtil;

public class StatusAddJobBean extends QuartzJobBean {

	private static Logger logger = LoggerFactory.getLogger(StatusAddJobBean.class);

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
		try {
			String statusInfo = GXProber.proberTool.getSysInfo();
			Map<String, Object> data = null;
			data = (Map<String, Object>) JSONUtil.fromJson(statusInfo, Map.class);	
			
			Date now = new Date();
			Status status = new Status();
			status.setIp(GXProber.address);
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			status.setBucket(df.format(now));
			status.setTime(now);
			status.setStatus(getState());
			status.setCpu(Double.parseDouble(data.get("cpu_usage").toString()));
			status.setDisk_free(Double.parseDouble(data.get("disk_free").toString()));
			status.setDisk_usage(Double.parseDouble(data.get("disk_usage").toString()));
			status.setRam_free(Double.parseDouble(data.get("mem_free").toString()));
			status.setRam_usage(Double.parseDouble(data.get("mem_used").toString()));
			status.setWf(Double.parseDouble(data.get("ClientRequestWriteFailures").toString()));
			status.setWl(Double.parseDouble(data.get("ClientRequestWriteLatency").toString()));
			status.setWt(Double.parseDouble(data.get("ClientRequestWriteTimeouts").toString()));
			status.setRf(Double.parseDouble(data.get("ClientRequestReadFailures").toString()));
			status.setRl(Double.parseDouble(data.get("ClientRequestReadLatency").toString()));
			status.setRt(Double.parseDouble(data.get("ClientRequestReadTimeouts").toString()));
			GXProber.proberDao.addStatus(status);

			double mem_free = Double.parseDouble(data.get("mem_free").toString());
			double mem_usage = Double.parseDouble(data.get("mem_used").toString());
			DecimalFormat df1 = new DecimalFormat("#.0");
			if (mem_usage / (mem_free + mem_usage) >= 0.8) {
				GXProber.proberDao.addLog(new Date(), "严重",
						"节点(127.0.0.1) 的内存使用警告：" + df1.format(mem_usage*100.0 / (mem_free + mem_usage)) + "%", "");
			} else if (mem_usage / (mem_free + mem_usage) >=0.9) {
				GXProber.proberDao.addLog(new Date(), "严重",
						"节点(127.0.0.1) 的内存使用警告：" + df1.format(mem_usage*100.0 / (mem_free + mem_usage)) + "%", "");

			} else if (mem_usage / (mem_free + mem_usage) >=0.95) {
				GXProber.proberDao.addLog(new Date(), "紧急",
						"节点(127.0.0.1) 的内存使用警告：" + df1.format(mem_usage*100.0 / (mem_free + mem_usage)) + "%", "");

			}

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

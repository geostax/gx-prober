package com.geostax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geostax.prober.biz.ExecutorBiz;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.model.TriggerParam;
import com.geostax.prober.rpc.netcom.NetComClientProxy;

public class ProberTest {
	private static Logger logger = LoggerFactory.getLogger(ProberTest.class);

	public static void main(String[] args) {
		TriggerParam triggerParam = new TriggerParam();
		triggerParam.setExecutorParams("Lalalalalala");
		triggerParam.setExecutorHandler("gxProberCommondHandler");
		triggerParam.setJobId(1);
		triggerParam.setLogId(1);
		String address="localhost:9999";
		ReturnT<String> runResult = null;
		try {
			ExecutorBiz executorBiz = (ExecutorBiz) new NetComClientProxy(ExecutorBiz.class, address).getObject();
			runResult = executorBiz.run(triggerParam);
			System.out.println(runResult);
		} catch (Exception e) {
			logger.error("", e);
			runResult = new ReturnT<String>(ReturnT.FAIL_CODE, "" + e);
		}
	}
}

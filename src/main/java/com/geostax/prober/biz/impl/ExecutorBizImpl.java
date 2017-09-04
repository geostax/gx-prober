package com.geostax.prober.biz.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geostax.prober.GXProber;
import com.geostax.prober.biz.ExecutorBiz;
import com.geostax.prober.enums.ExecutorBlockStrategyEnum;
import com.geostax.prober.handler.IJobHandler;
import com.geostax.prober.model.LogResult;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.model.TriggerParam;
import com.geostax.prober.thread.JobThread;

/**
 * Created by xuxueli on 17/3/1.
 */
public class ExecutorBizImpl implements ExecutorBiz {
	private static Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);

	@Override
	public ReturnT<String> beat() {
		return ReturnT.SUCCESS;
	}

	@Override
	public ReturnT<String> kill(int jobId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ReturnT<String> run(TriggerParam triggerParam) {

		JobThread jobThread = GXProber.loadJobThread(triggerParam.getJobId());
		IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;
		String removeOldReason = null;

		// valid old jobThread
		if (jobThread != null && jobHandler != null && jobThread.getHandler() != jobHandler) {
			// change handler, need kill old thread
			removeOldReason = "更新JobHandler或更换任务模式,终止旧任务线程";

			jobThread = null;
			jobHandler = null;
		}

		// valid handler
		if (jobHandler == null) {
			jobHandler = GXProber.loadJobHandler(triggerParam.getExecutorHandler());
			if (jobHandler == null) {
				return new ReturnT<String>(ReturnT.FAIL_CODE,
						"job handler [" + triggerParam.getExecutorHandler() + "] not found.");
			}
		}

		// executor block strategy
		if (jobThread != null) {
			ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum
					.match(triggerParam.getExecutorBlockStrategy(), null);
			if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
				// discard when running
				if (jobThread.isRunningOrHasQueue()) {
					return new ReturnT<String>(ReturnT.FAIL_CODE,
							"阻塞处理策略-生效：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
				}
			} else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
				// kill running jobThread
				if (jobThread.isRunningOrHasQueue()) {
					removeOldReason = "阻塞处理策略-生效：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();

					jobThread = null;
				}
			} else {
				// just queue trigger
			}
		}

		// replace thread (new or exists invalid)
		if (jobThread == null) {
			jobThread = GXProber.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
		}

		// push data to queue
		ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
		return pushResult;
	}

}

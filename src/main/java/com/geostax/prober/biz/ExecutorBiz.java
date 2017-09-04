package com.geostax.prober.biz;

import com.geostax.prober.model.LogResult;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.model.TriggerParam;

/**
 * Created by Xiao Fei on 17/3/1.
 * 
 * Util bean for RPC process
 * 
 */
public interface ExecutorBiz {

    /**
     * beat
     * @return
     */
    public ReturnT<String> beat();

    /**
     * kill
     * @param jobId
     * @return
     */
    public ReturnT<String> kill(int jobId);

    /**
     * log
     * @param logDateTim
     * @param logId
     * @param fromLineNum
     * @return
     */
    public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum);

    /**
     * run
     * @param triggerParam
     * @return
     */
    public ReturnT<String> run(TriggerParam triggerParam);

}

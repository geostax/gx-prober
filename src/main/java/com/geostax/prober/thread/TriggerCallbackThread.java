package com.geostax.prober.thread;

import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geostax.prober.model.HandleCallbackParam;
import com.geostax.prober.model.ReturnT;
import com.geostax.prober.util.AdminApiUtil;

/**
 * Created by xuxueli on 16/7/22.
 */
public class TriggerCallbackThread {
    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();
    public static TriggerCallbackThread getInstance(){
        return instance;
    }

    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<HandleCallbackParam>();

    private Thread triggerCallbackThread;
    private boolean toStop = false;
    public void start() {
        triggerCallbackThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(!toStop){
                    try {
                        HandleCallbackParam callback = getInstance().callBackQueue.take();
                        if (callback != null) {
                            // callback
                            try {
                                ReturnT<String> callbackResult = AdminApiUtil.callApiFailover(AdminApiUtil.CALLBACK, callback);
                                logger.info(">>>>>>>>>>> xxl-job callback, HandleCallbackParam:{}, callbackResult:{}", new Object[]{callback.toString(), callbackResult.toString()});
                            } catch (Exception e) {
                                logger.error(">>>>>>>>>>> xxl-job TriggerCallbackThread Exception:", e);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }
            }
        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.start();
    }
    public void toStop(){
        toStop = true;
    }

    public static void pushCallBack(HandleCallbackParam callback){
        getInstance().callBackQueue.add(callback);
        logger.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
    }

}

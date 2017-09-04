package com.geostax.prober.handler.impl;

import org.springframework.stereotype.Service;

import com.geostax.prober.handler.IJobHandler;
import com.geostax.prober.handler.JobHander;
import com.geostax.prober.model.ReturnT;


/**
 * 任务Handler的一个Demo（Bean模式）
 *
 * 开发步骤：
 * 1、继承 “IJobHandler” ；
 * 2、装配到Spring，例如加 “@Service” 注解；
 * 3、加 “@JobHander” 注解，注解value值为新增任务生成的JobKey的值;多个JobKey用逗号分割;
 * 4、执行日志：需要通过 "XxlJobLogger.log" 打印执行日志；
 *
 * @author xuxueli 2015-12-19 19:43:36
 */
@JobHander(value="gxProberCommondHandler")
@Service
public class GXProberCommondHandler extends IJobHandler {

	@Override
	public ReturnT<String> execute(String... params) throws Exception {
		System.out.println(">>>>>>>>>>>> Run gxProberCommondHandler: " +params);
		
		ReturnT<String> result=new ReturnT<>();
		result.setContent("Run gxProberCommondHandler Success!");
		result.setMsg("Run gxProberCommondHandler Success!");
		result.setCode(ReturnT.SUCCESS_CODE);
		
		
		return result;
		
		//return ReturnT.SUCCESS;
	}

}

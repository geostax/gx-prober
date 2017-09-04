package com.geostax.prober.dao;

import java.util.Date;

import com.geostax.prober.mapper.Current;
import com.geostax.prober.mapper.LogInfo;
import com.geostax.prober.mapper.Node;
import com.geostax.prober.mapper.Status;

public interface ProberDao {

	public void register(Node node);

	public void setCurrent(Current current);

	public void addStatus(Status status);
	
	public void addLog(LogInfo logInfo);
	
	public void addLog(Date date,String type,String msg,String info);
}

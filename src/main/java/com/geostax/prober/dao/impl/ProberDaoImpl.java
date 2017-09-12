package com.geostax.prober.dao.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.geostax.prober.CassandraManager;
import com.geostax.prober.dao.ProberDao;
import com.geostax.prober.mapper.Current;
import com.geostax.prober.mapper.LogInfo;
import com.geostax.prober.mapper.Node;
import com.geostax.prober.mapper.Status;

@Repository
public class ProberDaoImpl implements ProberDao {

	private static Logger logger = LoggerFactory.getLogger(ProberDaoImpl.class);
	@Resource
	CassandraManager cassandraManager;

	private MappingManager manager = null;

	@Override
	public void register(Node node) {
		if (manager == null)
			manager = new MappingManager(cassandraManager.getSession());
		Mapper<Node> mapper = manager.mapper(Node.class);
		logger.info(">>>> Reigser new Node: {}", new Object[] { node });
		mapper.save(node);
	}

	@Override
	public void setCurrent(Current current) {
		if (manager == null)
			manager = new MappingManager(cassandraManager.getSession());
		Mapper<Current> mapper = manager.mapper(Current.class);
		logger.info(">>>> Set Current Status: {}", new Object[] { current });
		mapper.save(current);

	}

	@Override
	public void addStatus(Status status) {
		if (manager == null)
			manager = new MappingManager(cassandraManager.getSession());
		Mapper<Status> mapper = manager.mapper(Status.class);
		logger.info(">>>> Add new Status: {}", new Object[] { status });
		mapper.save(status);

	}
	
	@Override
	public void addLog(LogInfo logInfo) {
		if (manager == null)
			manager = new MappingManager(cassandraManager.getSession());
		Mapper<LogInfo> mapper = manager.mapper(LogInfo.class);
		logger.info(">>>> Add new Status: {}", new Object[] { logInfo });
		mapper.save(logInfo);
		
	}
	
	@Override
	public void addLog(Date date,String type,String msg,String info){
		Session session = cassandraManager.getSession();
        session.execute("use gx_prober;");
       
        ResultSet set = session.execute("select * from node where ip=?;", cassandraManager.getHost());
        Row row = set.one();
        String dc=row.getString("datacenter");
        String rack=row.getString("rack");
        DateFormat df = new SimpleDateFormat("yyyy-MM");
        
        LogInfo logInfo=new LogInfo();
        logInfo.setDatacenter(dc);
        logInfo.setRack(rack);
        logInfo.setBucket(df.format(date));
        logInfo.setIp(cassandraManager.getHost());
        logInfo.setType(type);
        logInfo.setMsg(msg);
        logInfo.setInfo(info);
        logInfo.setTime(date);
        
        addLog(logInfo);
        
        
	}

}

CREATE TABLE gx_prober.node (
	ip text,
	cpu_core int,
	cpu_hz text,
	datacenter text,
	disk bigint,
	host_id text,
	name text,
	rack text,
	ram bigint,
	tokens int,
	PRIMARY KEY (ip)
);


CREATE TABLE gx_prober.current_status (
	ip text,
	cpu double,
	disk_free double,
	disk_usage double,
	ram_free double,
	ram_usage double,
	rf double,
	rl double,
	rt double,
	status text,
	time timestamp,
	wf double,
	wl double,
	wt double,
	PRIMARY KEY (ip)
);

CREATE TABLE gx_prober.status (
	ip text,
	bucket text,
	time timestamp,
	cpu double,
	disk_free double,
	disk_usage double,
	ram_free double,
	ram_usage double,
	rf double,
	rl double,
	rt double,
	status text,
	wf double,
	wl double,
	wt double,
	PRIMARY KEY (( ip, bucket ), time)
);


CREATE TABLE gx_prober.log (
	ip text,
	bucket text,
	time timestamp,
	dc text,
	info text,
	msg text,
	rack text,
	type text,
	PRIMARY KEY (( ip, bucket ), time)
) WITH CLUSTERING ORDER BY ( time DESC );





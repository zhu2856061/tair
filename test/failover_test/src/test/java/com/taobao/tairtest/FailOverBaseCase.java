/**
 * 
 */
package com.taobao.tairtest;

import com.ibm.staf.*;
import static org.junit.Assert.*;
import com.taobao.tairtest.ConfParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.BeforeClass;

public class FailOverBaseCase extends BaseTestCase {

	// db type
	protected static String db_type;
	// directory
	protected static String tair_bin;
	protected static String test_bin;
	protected static String test_bin1;
	protected static String test_bin2;
	// key words for log
	protected static String start_migrate;
	protected static String finish_migrate;
	protected static String finish_rebuild;
	// parameters
	protected static int down_time;
	protected static int ds_down_time;
	protected static String put_count;
	protected static float put_count_float;
	protected static String kv_name;
	// server list
	protected static List<String> csList;
	protected static List<String> dsList;
	protected static List<String> csList1;
	protected static List<String> csList2;
	protected static List<String> dsList1;
	protected static List<String> dsList2;
	// server name
	protected static String csname;
	protected static String dsname;
	protected static String csport;
	protected static String dsport;
	protected static String toolname;
	protected static String toolconf;
	// distinguish if mdb needs touch
	protected static int touch_flag;
	// server operation
	protected final static String groupconf = "etc/group.conf";
	protected final static String start = "start";
	protected final static String stop = "stop";
	protected final static String copycount = "_copy_count";
	// tool option
	protected final static String actiontype = "actiontype";
	protected final static String datasize = "datasize";
	protected final static String filename = "filename";
	protected final static String proxyflag = "proxyflag";
	protected final static String put = "put";
	protected final static String get = "get";
	protected final static String rem = "rem";
	protected final static String cs = "cs";
	protected final static String ds = "ds";
	// system option
	protected final static String local = "local";
	// judge measure
	protected final static float normSucRate = 0.99f;
	protected final static float migSucRate = 0.92f;

	@BeforeClass
	public static void baseBeforeClass() {
		ConfParser parser = new ConfParser("failover.conf");

		// Read public configuration
		readPublic(parser);

		// Read mdb configuration
		if ("mdb".equals(db_type))
			readMdbConf(parser);

		// Read ldb configuration
		else if ("ldb".equals(db_type))
			readLdbConf(parser);

		// Read rdb configuration
		else if ("rdb".equals(db_type))
			readRdbConf(parser);

		// Read invalid configuration
		else if ("invalid".equals(db_type))
			readInvalidConf(parser);
		else
			assertTrue(
					"db_type error! it must be mdb | ldb | rdb | invalid, but actual "
							+ db_type, false);
	}

	private static void readPublic(ConfParser ps) {
		db_type = ps.getValue("public", "db_type");
		tair_bin = ps.getValue("public", "tair_bin");
		test_bin = ps.getValue("public", "test_bin");
		start_migrate = ps.getValue("public", "start_migrate");
		finish_migrate = ps.getValue("public", "finish_migrate");
		finish_rebuild = ps.getValue("public", "finish_rebuild");
		down_time = Integer.parseInt(ps.getValue("public", "down_time"));
		ds_down_time = Integer.parseInt(ps.getValue("public", "ds_down_time"));
		put_count = ps.getValue("public", "put_count");
		put_count_float = (float) Integer.parseInt(put_count);
		kv_name = ps.getValue("public", "kv_name");
		csname = ps.getValue("public", "csname");
		dsname = ps.getValue("public", "dsname");
		csport = ps.getValue("public", "csport");
		dsport = ps.getValue("public", "dsport");
		toolname = ps.getValue("public", "toolname");
		toolconf = ps.getValue("public", "toolconf");
	}

	private static void readMdbConf(ConfParser ps) {
		String[] csArray = ps.getValue("mdb", "cs_list").split("\\,");
		if (csArray.length != 0)
			csList = Arrays.asList(csArray);
		else
			assertTrue("csList null! it must be at least one ip!", false);

		String[] dsArray = ps.getValue("mdb", "ds_list").split("\\,");
		if (dsArray.length != 0)
			dsList = Arrays.asList(dsArray);
		else
			assertTrue("dsList null! it must be at least one ip!", false);

		touch_flag = Integer.parseInt(ps.getValue("mdb", "touch_flag"));
	}

	private static void readLdbConf(ConfParser ps) {
	}

	private static void readRdbConf(ConfParser ps) {
	}

	private static void readInvalidConf(ConfParser ps) {
		String[] csArray1 = ps.getValue("invalid", "cs_list1").split("\\,");
		if (csArray1.length != 0)
			csList1 = Arrays.asList(csArray1);
		else
			assertTrue("csList1 null! it must be at least one ip!", false);

		String[] csArray2 = ps.getValue("invalid", "cs_list2").split("\\,");
		if (csArray2.length != 0)
			csList2 = Arrays.asList(csArray2);
		else
			assertTrue("csList2 null! it must be at least one ip!", false);

		String[] dsArray1 = ps.getValue("invalid", "ds_list1").split("\\,");
		if (dsArray1.length != 0)
			dsList1 = Arrays.asList(dsArray1);
		else
			assertTrue("dsList1 null! it must be at least one ip!", false);

		String[] dsArray2 = ps.getValue("invalid", "ds_list2").split("\\,");
		if (dsArray2.length != 0)
			dsList2 = Arrays.asList(dsArray2);
		else
			assertTrue("dsList2 null! it must be at least one ip!", false);

		test_bin1 = ps.getValue("invalid", "test_bin1");
		test_bin2 = ps.getValue("invalid", "test_bin2");
	}

	/**
	 * @param machine
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	protected boolean control_cs(String machine, String opID, int type) {
		log.debug("control cs:" + machine + " " + opID + " type=" + type);
		boolean ret = false;
		String cmd = "cd " + tair_bin + " && ./tair.sh " + opID
				+ "_cs && sleep 5";
		if (opID.equals(stop) && type == 1)
			cmd = "killall -9 " + csname;
		executeShell(stafhandle, machine, cmd);
		cmd = "ps -ef|grep " + csname + "|wc -l";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0) {
			log.error("result.rc != 0! " + result.rc);
			ret = false;
		} else {
			String stdout = getShellOutput(result);
			log.debug("------------cs ps result--------------" + stdout);
			if (opID.equals(start)
					&& (new Integer(stdout.trim())).intValue() != 3) {
				ret = false;
			} else if (opID.equals(stop)
					&& (new Integer(stdout.trim())).intValue() != 2) {
				ret = false;
			} else {
				ret = true;
			}
		}
		return ret;
	}

	/**
	 * @param machine
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	protected boolean control_ds(String machine, String opID, int type) {
		log.debug("control ds:" + machine + " " + opID + " type=" + type);
		boolean ret = false;
		String cmd = "cd " + tair_bin + " && ./tair.sh " + opID + "_ds ";
		int expectNum = 0;
		if (opID.equals(stop)) {
			expectNum = 2;
		} else if (opID.equals(start)) {
			expectNum = 3;
		}
		if (opID.equals(stop) && type == 1)
			cmd = "killall -9 " + dsname;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		
		int waittime = 0;
		cmd = "ps -ef|grep " + dsname + "|wc -l";
		while (waittime < 110) {
			result = executeShell(stafhandle, machine, cmd);
			if (result.rc != 0) {
				log.error("result.rc not 0! " + result.rc);
				ret = false;
			} else {
				String stdout = getShellOutput(result);
				if ((new Integer(stdout.trim())).intValue() == expectNum) {
					log.debug("------------ds ps result--------------" + stdout);
					ret = true;
					break;
				} else {
					ret = false;
					waitto(1);
					waittime++;
				}
			}
		}
		return ret;
	}

	/**
	 * @param cs_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	protected boolean batch_control_cs(List<String> cs_group, String opID, int type) {
		boolean ret = false;
		for (Iterator<String> it = cs_group.iterator(); it.hasNext();) {
			if (!control_cs(it.next(), opID, type)) {
				ret = false;
				break;
			} else
				ret = true;
		}
		return ret;
	}

	/**
	 * @param ds_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	protected boolean batch_control_ds(List<String> ds_group, String opID, int type) {
		boolean ret = false;
		for (Iterator<String> it = ds_group.iterator(); it.hasNext();) {
			if (!control_ds(it.next(), opID, type)) {
				ret = false;
				break;
			} else
				ret = true;
		}
		return ret;
	}

	/**
	 * @param cs_group
	 * @param ds_group
	 * @param opID
	 * @param type
	 *            0:normal 1:force
	 * @return
	 */
	protected boolean control_cluster(List<String> cs_group,
			List<String> ds_group, String opID, int type) {
		boolean ret = false;
		if (!batch_control_ds(ds_group, opID, type)
				|| !batch_control_cs(cs_group, opID, type))
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean clean_data(String machine) {
		boolean ret = false;
		String cmd = "cd " + tair_bin + " && ./tair.sh clean";
		STAFResult rst = executeShell(stafhandle, machine, cmd);
		if (rst.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean clean_tool(String machine) {
		boolean ret = false;
		killall_tool_proc();
		String cmd = "cd " + test_bin + " && ";
		cmd += "./clean.sh";
		STAFResult rst = executeShell(stafhandle, machine, cmd);
		if (rst.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean clean_bin_tool(String machine) {
		boolean ret = false;
		killall_tool_proc();
		String cmd = "cd " + tair_bin + "tools/" + " && ";
		cmd += "./clean.sh";
		STAFResult rst = executeShell(stafhandle, machine, cmd);
		if (rst.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean execute_data_verify_tool() {
		log.debug("start verify tool,run batchData");
		boolean ret = false;
		String cmd = "cd " + test_bin + " && ";
		cmd += "./batchData.sh";
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean execute_stress_tool(int cnt) {
		log.debug("start stress tool");
		boolean ret = false;
		String cmd = "cd " + test_bin + " && ";
		cmd += "./debug.sh " + cnt;
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean killall_tool_proc() {
		log.debug("force kill all data tool process");
		boolean ret = false;
		String cmd = "killall -9 " + toolname;
		STAFResult result = executeShell(stafhandle, "local", cmd);
		if (result.rc != 0)
			ret = false;
		else
			ret = true;
		return ret;
	}

	protected boolean batch_clean_data(List<String> machines) {
		boolean ret = true;
		for (Iterator<String> it = machines.iterator(); it.hasNext();) {
			if (!clean_data(it.next()))
				ret = false;
		}
		return ret;
	}

	protected boolean reset_cluster(List<String> csList, List<String> dsList) {
		boolean ret = false;
		log.debug("stop and clean cluster!");
		if (control_cluster(csList, dsList, stop, 1)
				&& batch_clean_data(csList) && batch_clean_data(dsList))
			ret = true;
		return ret;
	}

	protected int check_process(String machine, String prname) {
		log.debug("check process " + prname + " on " + machine);
		int ret = 0;
		String cmd = "ps -ef|grep " + prname + "|wc -l";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	protected int check_keyword(String machine, String keyword, String logfile) {
		int ret = 0;
		String cmd = "grep \"" + keyword + "\" " + logfile + " |wc -l";
		log.debug("check keyword:" + cmd + " on " + machine + " in file:"
				+ logfile);
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
				log.debug("count=" + ret);
			} catch (Exception e) {
				log.error("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	protected int touch_file(String machine, String file) {
		int ret = 0;
		String cmd = "touch " + file;
		log.debug("touch file:" + file + " on " + machine);
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = -1;
		return ret;
	}

	/**
	 * @param machine
	 *            machine which you want to operate
	 * @param confname
	 *            file which you want to modify , should include full path
	 * @param key
	 *            key which you want to modify
	 * @param value
	 *            value which you want to change
	 * @return
	 */
	protected boolean modify_config_file(String machine, String confname,
			String key, String value) {
		log.debug("change file:" + confname + " on " + machine + " key=" + key
				+ " value=" + value);
		boolean ret = false;
		String cmd = "sed -i \"s/" + key + "=.*$/" + key + "=" + value + "/\" "
				+ confname + " && grep \"" + key + "=.*$\" " + confname
				+ "|awk -F\"=\" \'{print $2}\'";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			String stdout = getShellOutput(result);
			if (stdout.trim().equals(value))
				ret = true;
			else
				ret = false;
		}
		return ret;
	}

	protected boolean batch_modify(List<String> machines, String confname,
			String key, String value) {
		boolean ret = true;
		for (Iterator<String> it = machines.iterator(); it.hasNext();) {
			if (!modify_config_file(it.next(), confname, key, value)) {
				ret = false;
				break;
			}
		}
		return ret;
	}

	protected void shutoff_net(String machine1, String port1, String machine2,
			String port2) {
		log.debug("shut off net between " + machine1 + ":" + port1 + " and "
				+ machine2 + ":" + port2);
		String cmd = "ssh root@localhost " + "\"/sbin/iptables -A OUTPUT -d "
				+ machine2 + " -p tcp --dport " + port2
				+ " -j DROP && /sbin/iptables-save\"";
		STAFResult result = executeShell(stafhandle, machine1, cmd);
		if (result.rc != 0) {
			fail("shut off net between " + machine1 + " and " + machine2
					+ " failed!");
		} else {
			cmd = "ssh root@localhost " + "\"/sbin/iptables -A INPUT -s "
					+ machine2 + " -p tcp --dport " + port1
					+ " -j DROP && /sbin/iptables-save\"";
			result = executeShell(stafhandle, machine1, cmd);
			if (result.rc != 0) {
				fail("shut off net between " + machine1 + " and " + machine2
						+ " failed!");
			} else {
				log.info("shut off net between " + machine1 + " and "
						+ machine2 + " successful!");
			}
		}
	}

	protected boolean recover_net(String machine) {
		log.debug("recover net on " + machine);
		boolean ret = true;
		String cmd = "ssh root@localhost " + "\"/sbin/iptables -F\"";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}

	protected boolean batch_recover_net(List<String> machines) {
		boolean ret = true;
		for (Iterator<String> it = machines.iterator(); it.hasNext();) {
			if (!recover_net(it.next())) {
				ret = false;
				break;
			}
		}
		return ret;
	}

	protected boolean execute_tool_on_mac(String machine, String option) {
		log.debug("start " + option + " on " + machine);
		if (!modify_config_file(machine, tair_bin + "tools/" + toolconf,
				"actiontype", option))
			fail("modify configure file failed");
		if (!modify_config_file(machine, tair_bin + "tools/" + toolconf,
				"datasize", put_count))
			fail("modify configure file failed");
		if (!modify_config_file(machine, tair_bin + "tools/" + toolconf,
				"filename", "read.kv"))
			fail("modify configure file failed");

		boolean ret = true;
		String cmd = "cd " + tair_bin + "tools/" + "&& ./batchData.sh";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}

	protected void wait_tool_on_mac(String machine) {
		log.error("start wait on " + machine);
		int count = 0;
		while (check_process(machine, toolname) != 2) {
			waitto(16);
			if (++count > 150)
				break;
		}
		if (count > 150)
			fail("wait time out on " + machine + "!");
		log.error("wait success on " + machine);
	}

	protected boolean execute_clean_on_cs(String machine) {
		log.debug("clean kv and log files on " + machine);
		boolean ret = true;
		String cmd = "cd " + tair_bin + "tools/" + "&& ./clean.sh";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}

	protected void wait_keyword_equal(String machine, String keyword,
			int expect_count) {
		int count = 0;
		while (check_keyword(machine, keyword, tair_bin + "logs/config.log") != expect_count) {
			waitto(2);
			if (++count > 10)
				break;
		}
		if (count > 10)
			fail("wish the count of " + keyword + " on " + machine
					+ " not change, but changed!");
		log.error("the count of " + keyword + " on " + machine + " is right!");
	}

	protected void wait_keyword_change(String machine, String keyword,
			int base_count) {
		int count = 0;
		while (check_keyword(machine, keyword, tair_bin + "logs/config.log") <= base_count) {
			waitto(2);
			if (++count > 10)
				break;
		}
		if (count > 10)
			fail("wish the count of " + keyword + " on " + machine
					+ " change, but not changed!");
		log.error("the count of " + keyword + " on " + machine + " is right!");
	}

	protected int getVerifySuccessful(String machine) {
		int ret = 0;
		String verify = "tail -10 " + tair_bin + "tools/"
				+ "datadbg0.log|grep \"Successful\"|awk -F\" \" \'{print $3}\'";
		log.debug("do verify on " + machine);
		STAFResult result = executeShell(stafhandle, machine, verify);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
				log.info("verify result: " + ret);
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		log.error(ret);
		return ret;
	}

	protected boolean copy_file(String source_machine, String filename,
			String target_machine) {
		log.error("Copy " + filename + " from " + source_machine + " to "
				+ target_machine);
		boolean ret = true;
		String cmd = "scp " + filename + " " + target_machine + ":" + test_bin;
		STAFResult result = executeShell(stafhandle, source_machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}

	protected boolean execute_shift_tool(String machine, String opID) {
		log.error("Copy " + opID + " files on " + machine);
		boolean ret = true;
		String cmd = "cd " + test_bin + " && ./shift.sh " + opID;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		return ret;
	}

	protected boolean comment_line(String machine, String file, String keyword,
			String comment) {
		boolean ret = false;
		String cmd = "sed -i \'s/.*" + keyword + "/" + comment + "&/\' " + file;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			// String stdout=getShellOutput(result);
			ret = true;
		}
		return ret;
	}

	protected boolean uncomment_line(String machine, String file, String keyword,
			String comment) {
		boolean ret = false;
		String cmd = "sed -i \'s/" + comment + "*\\(.*" + keyword
				+ ".*$\\)/\\1/\' " + file;
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0)
			ret = false;
		else {
			// String stdout=getShellOutput(result);
			ret = true;
		}
		return ret;
	}

	protected boolean batch_uncomment(List<String> machines, String confname,
			List<String> keywords, String comment) {
		boolean ret = true;
		for (Iterator<String> ms = machines.iterator(); ms.hasNext();) {
			String cms = ms.next();
			for (Iterator<String> ks = keywords.iterator(); ks.hasNext();) {
				if (!uncomment_line(cms, confname, ks.next(), comment))
					ret = false;
			}
		}
		return ret;
	}

	protected int getVerifySuccessful() {
		int ret = 0;
		String verify = "cd " + test_bin + " && ";
		verify += " tail -10 datadbg0.log|grep \"Successful\"|awk -F\" \" \'{print $3}\'";
		log.debug("do verify on local");
		STAFResult result = executeShell(stafhandle, "local", verify);
		if (result.rc != 0) {
			log.debug("get result " + result);
			ret = -1;
		} else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
				log.info("verify result: " + ret);
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}

	protected int getDSFailNum(String dsName) {
		int ret = 0;
		String verify = "cd " + test_bin + " && ";
		verify += "cat datadbg0.log|grep \"get failure: " + dsName + "\"|wc -l";
		STAFResult result = executeShell(stafhandle, "local", verify);
		if (result.rc != 0)
			ret = -1;
		else {
			String stdout = getShellOutput(result);
			try {
				ret = (new Integer(stdout.trim())).intValue();
			} catch (Exception e) {
				log.debug("get verify exception: " + stdout);
				ret = -1;
			}
		}
		return ret;
	}
	
	protected void batchControlServer(List<String> serverList,
			String serverType, String option, int type) {
		log.info("start batch " + option + " " + serverType + " list!");
		boolean ret = true;
		ExecutorService exec = Executors.newFixedThreadPool(serverList.size());
		ArrayList<Future<Boolean>> resultList = new ArrayList<Future<Boolean>>();
		for (int i = 0; i < serverList.size(); i++) {
			resultList.add(exec.submit(new ControlServer(serverList.get(i),
					serverType, option, type)));
		}
		boolean waitFlag = true;
		int waitLimit = 0;
		while (waitFlag && waitLimit < 10) {
			waitFlag = false;
			waitto(1);
			waitLimit++;
			for(int j = 0; j < serverList.size(); j++) {
				if(!resultList.get(j).isDone()) {
					waitFlag = true;
					break;
				}
			}
		}
		exec.shutdown();
		
		for(int k = 0; k < serverList.size(); k++) {
			Boolean retThread;
			try {
				retThread = resultList.get(k).get();
				if(retThread.booleanValue())
					log.debug(option + " " + serverType + " on " + serverList.get(k) + " successful!");
				else
					log.debug(option + " " + serverType + " on " + serverList.get(k) + " failed!");
				ret = retThread.booleanValue() && ret;
			} catch (InterruptedException e) {
				e.printStackTrace();
				fail("exception occured while get result on " + serverList.get(k));
			} catch (ExecutionException e) {
				e.printStackTrace();
				fail("exception occured while get result on " + serverList.get(k));
			}
		}
		String listName = "";
		for(String ip : serverList) {
			listName = listName + ip + " ";
		}
		if(!ret)
			fail("batch " + option + " " + serverType + " failed! " + listName);
		else
			log.info("batch " + option + " " + serverType + " successful! " + listName);
	}
}

class ControlServer extends BaseTestCase implements Callable<Boolean> {

	private String machine;
	private String serverType;
	private String serverName;
	private String option;
	private int type;
	
	public ControlServer(String machine, String serverType, String option, int type) {
		this.machine = machine;
		this.serverType = serverType;
		this.serverName = this.serverType.equals("cs") ? FailOverBaseCase.csname
				: FailOverBaseCase.dsname;
		this.option = option;
		this.type = type;
	}

	public Boolean call() throws Exception {
		boolean ret = false;
		String cmd = "cd " + FailOverBaseCase.tair_bin + " && ./tair.sh " + option
				+ "_" + serverType;
		if (option.equals(FailOverBaseCase.stop) && type == 1)
			cmd = "killall -9 " + serverName + " && sleep 1";
		executeShell(stafhandle, machine, cmd);
		cmd = "ps -ef|grep " + serverName + "|wc -l";
		STAFResult result = executeShell(stafhandle, machine, cmd);
		if (result.rc != 0) {
			log.error(machine + " result.rc not 0! " + result.rc);
			ret = false;
		} else {
			String stdout = getShellOutput(result);
			if (option.equals(FailOverBaseCase.start)
					&& (new Integer(stdout.trim())).intValue() != 3) {
				ret = false;
			} else if (option.equals(FailOverBaseCase.stop)
					&& (new Integer(stdout.trim())).intValue() != 2) {
				ret = false;
			} else {
				ret = true;
			}
		}
		return ret;
	}
}

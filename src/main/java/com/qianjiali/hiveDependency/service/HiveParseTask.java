package com.qianjiali.hiveDependency.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qianjiali.hiveDependency.entity.HiveConfig;
import com.qianjiali.hiveDependency.entity.ParseNode;
import com.qianjiali.hiveDependency.entity.ScriptItem;
import com.qianjiali.hiveDependency.utils.HdfsFileUtils;
import com.qianjiali.hiveDependency.utils.TxtFileUtils;

public class HiveParseTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(HiveParseTask.class);

	private static HiveConfig hiveParseConfig = HiveConfig.getInstance();

	private Path path;

	private CountDownLatch cdl;

	@Override
	public void run() {
		String scriptname = path.getName();
		System.out.println("The currently parsed script is:" + scriptname);
		try {
			List<String> parseScripts = HdfsFileUtils.getFileCotentByPath(path);
			if (parseScripts.size() > 0) {
				if (parseScripts.size() == 1) {
					String relationName = scriptname.substring(0, scriptname.indexOf(".sql")) + ".txt";
					writetohdfs(scriptname, parseScripts.get(0), relationName);
				} else {
					for (int i = 0; i < parseScripts.size(); i++) {
						String relationName = scriptname.substring(0, scriptname.indexOf(".sql")) + "_(" + i + ")"
								+ ".txt";
						writetohdfs(scriptname, parseScripts.get(i), relationName);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("The currently parsed script " + scriptname + " is error====>" + e.getMessage());
		} finally {
			try {
				cdl.countDown();
			} catch (Exception e) {
				System.out.println("close the hdfs stream is error:" + e.toString());
			}
		}
	}

	public void writetohdfs(String scriptname, String parseScript, String relationName) throws Exception {
		ScriptItem item = new ScriptItem(parseScript);
		ParseNode node = item.getParseNode();
		String noderelation = TxtFileUtils.getHiveNodeRelation(node, scriptname);
		String relationWriteUrl = hiveParseConfig.getHdfsDest() + "/" + relationName;
		HdfsFileUtils.writeFile2Hdfs(relationWriteUrl, noderelation);
	}
	

	public HiveParseTask(Path path, CountDownLatch cdl) {
		super();
		this.path = path;
		this.cdl = cdl;
	}

}

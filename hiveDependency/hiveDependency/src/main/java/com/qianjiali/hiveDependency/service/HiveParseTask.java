package com.qianjiali.hiveDependency.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qianjiali.hiveDependency.entity.HiveConfig;
import com.qianjiali.hiveDependency.entity.ParseNode;
import com.qianjiali.hiveDependency.entity.ScriptContent;
import com.qianjiali.hiveDependency.entity.ScriptInfo;
import com.qianjiali.hiveDependency.entity.ScriptItem;
import com.qianjiali.hiveDependency.utils.DateUtil;
import com.qianjiali.hiveDependency.utils.HdfsFileUtils;
import com.qianjiali.hiveDependency.utils.HiveScriptFilterUtils;
import com.qianjiali.hiveDependency.utils.TxtFileUtils;

public class HiveParseTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(HiveParseTask.class);

	private static HiveConfig hiveParseConfig = HiveConfig.getInstance();

	private FileSystem fileSystem;

	private Path path;

	private CountDownLatch cdl;

	@Override
	public void run() {
		FSDataInputStream fin = null;
		BufferedReader reader = null;
		String scriptname = path.getName();
		System.out.println("The currently parsed script is:" + scriptname);
		try {
			fin = fileSystem.open(path);
			reader = new BufferedReader(new InputStreamReader(fin, "UTF-8"));
			List<String> parseScripts = HiveScriptFilterUtils.filterScriptLine(reader,scriptname);
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
				HdfsFileUtils.closeHdfs(fin, null, reader);
				cdl.countDown();
			} catch (Exception e) {
				System.out.println("close the hdfs stream is error:" + e.toString());
			}
		}
	}

	public void setScriptContextInfo(Path path) {
		String scriptname = path.getName();
		ScriptInfo scriptInfo = new ScriptInfo();
		try {
			FileStatus fileStatus = HdfsFileUtils.fileSystem.getFileStatus(path);
			scriptInfo.setSize(Double.valueOf(fileStatus.getLen()/1000.0)+"KB");
			scriptInfo.setLastmotifytime(DateUtil.LongToDateString(fileStatus.getModificationTime()));
			scriptInfo.setUsername(fileStatus.getOwner());
			scriptInfo.setUsergroup(fileStatus.getGroup());
			scriptInfo.setUserauthority(String.valueOf(fileStatus.getPermission().toShort()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		ScriptContent.scriptMap.put(scriptname, scriptInfo);
	}

	public void writetohdfs(String scriptname, String parseScript, String relationName) throws Exception {
		FSDataOutputStream fout = null;
		try {
			String relationWriteUrl = hiveParseConfig.getHdfsDest() + "/" + relationName;
			System.out.println("The script analysis result is written to the address:" + relationWriteUrl);
			logger.info("The script analysis result is written to the address:" + relationWriteUrl);
			Path dest = new Path(relationWriteUrl);
			fout = fileSystem.create(dest, true);
			ScriptItem item = new ScriptItem(parseScript);
			ParseNode node = item.getParseNode();
			String noderelation = TxtFileUtils.getHiveNodeRelation(node, scriptname);
			fout.write(noderelation.getBytes(), 0, noderelation.getBytes().length);
			fout.flush();
		} catch (Exception e) {
			System.out.println("The currently parsed script " + scriptname + " is error" + e.toString());
		} finally {
			try {
				HdfsFileUtils.closeHdfs(null, fout, null);
			} catch (Exception e) {
				System.out.println("close the hdfs stream is error:" + e.toString());
			}
		}
	}

	public HiveParseTask(FileSystem fileSystem, Path path, CountDownLatch cdl) {
		super();
		this.fileSystem = fileSystem;
		this.path = path;
		this.cdl = cdl;
		setScriptContextInfo(path);
	}

}
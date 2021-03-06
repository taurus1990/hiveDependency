package com.qianjiali.hiveDependency.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.log4j.PropertyConfigurator;
import org.codehaus.groovy.transform.tailrec.ReturnStatementToIterationConverter;

import com.qianjiali.hiveDependency.entity.HiveConfig;
import com.qianjiali.hiveDependency.utils.InitHdfsConfig;

public class PropertiesUtil {

	private static Log logger = LogFactory.getLog(PropertiesUtil.class);
	private static Properties pros = new Properties();

	private static HiveConfig conf = HiveConfig.getInstance();

	public static HiveConfig initProperties(String rootPath) throws IOException {
		System.out.println("开始加载配置资源....................");
		//String rootPath = System.getProperty("user.dir");
		System.out.println("资源根路径是======================>>>>>" + rootPath);
		Path resourcePath = new Path(rootPath);
		FSDataInputStream inputStream = null;
		inputStream = HdfsFileUtils.fileSystem.open(resourcePath);				
		//FSDataInputStream is = null;
		
		try {
			//File file = new File(resourcePath);
			//is = new FileInputStream(inputSteam);	
			pros.load(inputStream);
			conf.setHdfsConfig(pros.getProperty("hdfs_config_url"));
			conf.setHdfsDest(pros.getProperty("hdfs_script_parse_relation_map_url"));
			conf.setPatternInsert(pros.getProperty("pattern_insert"));
			conf.setPatternPartition(pros.getProperty("pattern_partition"));
			conf.setPatternComments(pros.getProperty("pattern_comments"));
			conf.setHiveDriver(pros.getProperty("hive_driver"));
			conf.setHiveUrl(pros.getProperty("hive_url"));
			conf.setHiveUserName(pros.getProperty("hive_user_name"));
			conf.setHivePassword(pros.getProperty("hive_password"));
			conf.setCreateNewPartitionSql(pros.getProperty("create_new_partition_sql"));
			conf.setDeletePartitionSql(pros.getProperty("delete_partition_sql"));
			conf.setScriptContentHdfsWriteUrl(pros.getProperty("hdfs_script_detail_url"));
			conf.setCreateScriptDetailPartitionSql(pros.getProperty("create_script_detail_partition_sql"));
			conf.setDeleteScriptDetailPartitionSql(pros.getProperty("delete_script_detail_partition_sql"));
			conf.setRunningEnv(pros.getProperty("running_env"));
			System.out.println("配置资源加载完成....................");
			return conf;
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("资源文件找不到");
		}
		return null;
	}

	public static void setHiveParsePartition(String partitionStr) {
		conf.setPartitionStr(partitionStr);
	}
}

package com.sw.bi.scheduler.background.javatype.check;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sw.bi.scheduler.background.taskexcuter.xml.XmlCreator;
import com.sw.bi.scheduler.background.util.BeanFactory;
import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.service.TaskService;
import com.sw.bi.scheduler.util.DateUtil;

//     /home/tools/scheduler/scheduler_test jar   /home/tools/scheduler/scheduler_test.jar com.sw.bi.scheduler.background.javatype.check.CheckTaskException today
@Component
public class CheckTaskXmlCreateTest {

	@Autowired
	private TaskService taskService;

	protected Task currentTask;
	
	public static void main(String[] args) throws Exception {
		Date taskDate = DateUtil.getToday();
//		Date taskDate = DateUtil.getYesterday();
		CheckTaskXmlCreateTest.getCheckTaskException().getRequireParams(taskDate);
		
	}
	
	private void getRequireParams(Date taskDate) throws Exception {
		List<Task> tasksByTaskDate = taskService.getInitializeTasksByTaskDate(taskDate);
		for(Task task: tasksByTaskDate){
			System.out.println("datax job begin...");
			XmlCreator xmlCreator = new XmlCreator();
			System.out.println("begin to create xml file...");
			@SuppressWarnings("unused")
			String XML = xmlCreator.createByTask(task);
			System.out.println("最后标准的XML文件为：" + XML);
			System.out.println("xml file is created...");
			BufferedReader br = new BufferedReader(new StringReader(XML));
			File file = new File("d:/home/hbase2file.xml");
			if(!file.exists()){
				file.createNewFile();
			}
//			PrintWriter w = new PrintWriter(file);
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
			int length = 0;
			char[] buffer = new char[2048]; 
			while((length = br.read(buffer)) != -1){
//				w.print(buffer);
//				w.flush();
				out.write(buffer, 0, length);
				out.flush();
			}
//			w.close();
			out.close();
		}
		
	}

	private static CheckTaskXmlCreateTest getCheckTaskException() {
		return BeanFactory.getBean(CheckTaskXmlCreateTest.class);
	}
	
}

package com.sv.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.taglibs.standard.extra.spath.ParseException;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sv.bean.Task;
import com.sv.scheduler.ExecuterServiceProvider;
import com.sv.scheduler.ThreadPoolTaskSchedulerProvider;

@Service
public class TaskServiceImpl {

	@Autowired
	private ExecuterServiceProvider executerServiceProvider;

	@Autowired
	private ThreadPoolTaskSchedulerProvider threadPoolTaskSchedulerProvider;

	@Autowired
	JARExecuteService jARExecuteService;

	private Map<String, Task> taskContainerMap = new HashMap<>();

	Date date;

	CronExpression exp;

	TaskServiceImpl() {
		System.out.println("Adding dummy task.");
		Task tsk = new Task();
		
		tsk.setId(0);
		tsk.setName("MY DUMMY TASK");
		tsk.setLocation("C:/Users/CN003073/Desktop/Demo_Child.jar");
		tsk.setClassName("com.mmi.Child.Child");
		tsk.setMethodName("run");
		tsk.setActive(false);
		tsk.setCron("0/10 0 0 0 0 0");
		tsk.setScheduledFuture(null);
		
		/*tsk.setId(Integer.valueOf(PropertyFileUtility.getValue("Id")));
		tsk.setName(PropertyFileUtility.getValue("Name"));
		tsk.setLocation(PropertyFileUtility.getValue("Location"));
		tsk.setClassName(PropertyFileUtility.getValue("ClassName"));
		tsk.setMethodName(PropertyFileUtility.getValue("MethodName"));
		if(PropertyFileUtility.getValue("Active").equalsIgnoreCase("true")) {
			tsk.setActive(true);
		}else {
			tsk.setActive(false);
		}
		tsk.setCron(PropertyFileUtility.getValue("Cron"));
		tsk.setScheduledFuture(null);*/
		
		taskContainerMap.put(tsk.getName(), tsk);
	}

	public List<Task> listAll() {
		List<Task> ls = new ArrayList<Task>();
		taskContainerMap.forEach((key, value) -> {
			ls.add(value);
		});
		return ls;
	}

	public String enable(String taskName) {

		Task task = taskContainerMap.get(taskName);
		if (task == null) {
			return "Task not found/registered - " + taskName;
		}

		ThreadPoolTaskScheduler scheduler = threadPoolTaskSchedulerProvider.setScheduler();
		scheduler.initialize();
		ScheduledFuture<?> scheduledFuture = scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				System.out.println("The date is " + new Date());
				jARExecuteService.executeMethod(task, jARExecuteService.loadClass(task));
				
				//to check validity of cron
//				String a = "*/2 * * * * * 2019";
//				try {
//					exp = new CronExpression(a);
//					date = exp.getNextValidTimeAfter(new Date());
//					System.out.println(date); // null
//					exp = new CronExpression("*/2 * * * * * 2019");
//					date = exp.getNextValidTimeAfter(new Date());
//					System.out.println(date); // Tue Nov 04 19:20:30 PST 2014
//				} catch (java.text.ParseException e) {
//					e.printStackTrace();
//				}
			}
		}, new CronTrigger("*/2 * * * * MON-FRI"));
		/*
		 * CronTrigger cronTrigger= new CronTrigger(taskName); cronTrigger.
		 */
		task.setScheduledFuture(scheduledFuture);
		task.setActive(true);

		return "Task enabled - " + taskName;
	}

	public String disable(String taskName) {

		Task task = taskContainerMap.get(taskName);
		if (task == null) {
			return "Task not found/registered - " + taskName;
		}

		ScheduledFuture<?> scheduledFuture = task.getScheduledFuture();

		if (!scheduledFuture.isCancelled()) {
			scheduledFuture.cancel(true);
		}

		task.setActive(false);
		return "Task terminated - " + taskName;
	}

	public void register(Task task) {
		taskContainerMap.put(task.getName(), task);
	}

	public String validate(Task task) {
		if (task.getName() == null) {
			return "Name cannot be empty.";
		}
		task.setName(task.getName().replaceAll("\\s", ""));
		if (task.getName().isEmpty()) {
			return "Name cannot be empty.";
		}
		if (taskContainerMap.containsKey(task.getName())) {
			return "Name already registered.";
		}

		return null;
	}

}

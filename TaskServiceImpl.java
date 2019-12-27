package com.sv.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

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

	@Autowired
	TaskConfigLoader taskConfigLoader;

	private Map<String, Task> taskContainerMap = new HashMap<>();

	Date date;

	CronExpression exp;

	@PostConstruct
	public void loadTask() {
		ArrayList<Task> taskList = taskConfigLoader.xMLtoJAXBObject();
		for (Task task : taskList) {
			String message = validate(task);
			if (message == null) {
				register(task);
			} else {
				System.out.println("[TaskServiceImpl.loadTask()] " + message);
			}
		}
	}

	public List<Task> listAll() {
		List<Task> ls = new ArrayList<>();
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

			}
		}, new CronTrigger(task.getCron()));

		task.setScheduledFuture(scheduledFuture);
		task.setActive(true);
		
		System.out.println(task);
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

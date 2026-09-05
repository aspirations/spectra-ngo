package com.dertz.spectra.config;

import com.dertz.spectra.task.MonthEndPayrollJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

	@Bean
	public JobDetail monthEndPayrollJobDetail() {
		return JobBuilder.newJob(MonthEndPayrollJob.class)
				.withIdentity("monthEndPayrollJob")
				.storeDurably()
				.build();
	}

	@Bean
	public Trigger monthEndPayrollTrigger(JobDetail monthEndPayrollJobDetail) {
		return TriggerBuilder.newTrigger()
				.forJob(monthEndPayrollJobDetail)
				.withIdentity("monthEndPayrollTrigger")
				.withSchedule(CronScheduleBuilder.cronSchedule("0 0 22 L * ?"))
				.build();
	}
}

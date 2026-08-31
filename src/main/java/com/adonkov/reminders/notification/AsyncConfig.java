package com.adonkov.reminders.notification;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Pool that actually delivers notifications.
     * <p>
     * Sized from configuration rather than the CPU count because the work is IO bound --
     * each send is a network call, not computation. The queue is bounded and overflow falls
     * back to {@link ThreadPoolExecutor.CallerRunsPolicy}, which pushes the work onto the
     * scheduler thread and thereby throttles the scan loop instead of growing an unbounded
     * backlog of reminders in memory.
     */
    @Bean("notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor(NotificationProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workers());
        executor.setMaxPoolSize(properties.workers());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // let in-flight sends finish on shutdown so we don't lose a batch we already claimed
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * Dedicated scheduler so a slow scan can't block other scheduled work.
     * Spring's default scheduler is single threaded.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.initialize();
        return scheduler;
    }
}

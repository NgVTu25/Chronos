package com.job.distributed_job_scheduler.core.service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.job.distributed_job_scheduler.core.model.Job;
import com.job.distributed_job_scheduler.core.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreScheduler {

    private final JobRepository jobRepository;
    private final JobService jobService;
    private final JobExecutionService jobExecutionService;

    // Tên giả lập cho Node Scheduler hiện tại (trong thực tế có thể lấy từ IP/Hostname)
    private final String NODE_ID = "scheduler-node-" + UUID.randomUUID().toString().substring(0, 8);

    private final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    @Scheduled(fixedDelay = 5000)
    public void scheduleDueJobs() {
        Instant now = Instant.now();
        List<Job> dueJobs = jobRepository.findDueJobs(now);

        if (!dueJobs.isEmpty()) {
            log.info("Found {} due jobs to process", dueJobs.size());
        }

        for (Job job : dueJobs) {
            try {
                // 1. Cố gắng khóa job (tránh bị node khác tranh mất)
                boolean locked = jobService.lockJobForExecution(job.getId(), NODE_ID);
                if (!locked) continue;

                // 2. Tạo bản ghi thực thi (Execution) ném vào hàng đợi (trạng thái PENDING)
                jobExecutionService.createExecutionForJob(job);

                // 3. Tính toán thời gian chạy lần KẾ TIẾP dựa vào cron expression
                Instant nextRun = calculateNextRun(job.getCronExpression());

                // 4. Mở khóa và cập nhật next_run_at cho vòng lặp sau
                jobService.releaseLockAndScheduleNext(job.getId(), nextRun);

            } catch (Exception e) {
                log.error("Failed to schedule job {}: {}", job.getId(), e.getMessage());
                jobService.releaseLockAndScheduleNext(job.getId(), Instant.now().plusSeconds(60));
            }
        }
    }

    private Instant calculateNextRun(String cronExpression) {
        if (cronExpression == null || cronExpression.isEmpty()) return null;
        try {
            ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
            Optional<ZonedDateTime> nextExecution = executionTime.nextExecution(ZonedDateTime.now(ZoneId.systemDefault()));
            return nextExecution.map(ZonedDateTime::toInstant).orElse(null);
        } catch (Exception e) {
            log.error("Invalid cron expression: {}", cronExpression);
            return null;
        }
    }
}
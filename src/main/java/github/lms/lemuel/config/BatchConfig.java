package github.lms.lemuel.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * Spring Batch Configuration for K8s Multi-Pod Environment
 *
 * <p>DB 기반 JobRepository를 사용하여 여러 Pod에서 배치가 중복 실행되지 않도록 보장합니다.</p>
 * <ul>
 *   <li>BATCH_JOB_INSTANCE, BATCH_JOB_EXECUTION 테이블을 통한 Job 실행 이력 관리</li>
 *   <li>Pessimistic Lock을 통한 동시 실행 방지</li>
 *   <li>Spring Batch 5.x (Spring Boot 3.x)에서는 EnableBatchProcessing 없이도 자동 구성됨</li>
 * </ul>
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    /**
     * JobLauncher 구성 - 동기 실행 방식
     *
     * <p>K8s 환경에서 CronJob으로 실행될 경우, 동기 실행이 권장됩니다.</p>
     * <p>비동기 실행 시 Pod가 Job 완료 전 종료될 수 있습니다.</p>
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(new SyncTaskExecutor()); // 동기 실행
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}

/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa.jenkins;

import static org.awaitility.Awaitility.await;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Service;
import platform.qa.entities.WaitConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.junit.jupiter.api.Assertions;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;

/**
 * Client to work with Jenkins
 */
@Log4j2
public class JenkinsClient {
    @Setter
    private int poolIntervalTimeout = 15;
    @Setter
    private int waitTimeout = 30;
    @Setter
    @Getter
    private WaitConfiguration waitConfiguration;
    protected final JenkinsServer server;

    public JenkinsClient(Service jenkins) {
        try {
            waitConfiguration = WaitConfiguration
                    .newBuilder()
                    .setPoolIntervalTimeout(poolIntervalTimeout)
                    .setWaitTimeout(waitTimeout)
                    .build();
            server = new JenkinsServer(new URI(jenkins.getUrl()), jenkins.getLogin(), jenkins.getPassword());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Appeared issues with initiating JenkinsClient: ", e);
        }
    }

    @SneakyThrows
    public long startJob(String jobName) {
        log.info("Запуск Jenkins job-и за повернення її номеру " + jobName);
        waitForJobToBeAvailable(jobName);

        var build = server.getJob(jobName).build(
                Maps.newHashMap());

        final long[] number = new long[1];

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .ignoreExceptionsInstanceOf(NullPointerException.class)
                .untilAsserted(() -> {
                    var item = server.getQueueItem(build);
                    if (item.getWhy() != null)
                        Assertions.assertFalse((item.getWhy().contains("Finished waiting") ||
                                        item.getWhy().contains("is already in progress ")),
                                String.format("Waiting for job to be completed: %s", jobName));
                    number[0] = item.getExecutable().getNumber();

                });

        return number[0];
    }

    @SneakyThrows
    public long startJob(String jobName, HashMap<String, String> params) {
        log.info("Запуск Jenkins job-и за повернення її номеру");
        waitForJobToBeAvailable(jobName);

        try {
            var build = server.getJob(jobName).build(params);

            final long[] number = new long[1];

            await()
                    .pollInterval(1, TimeUnit.SECONDS)
                    .pollInSameThread()
                    .atMost(poolIntervalTimeout, TimeUnit.MINUTES)

                    .untilAsserted(() -> {
                        var item = server.getQueueItem(build);
                        if (item.getWhy() != null)
                            Assertions.assertFalse((item.getWhy().contains("Finished waiting") ||
                                            item.getWhy().contains("is already in progress ")),
                                    String.format("Waiting for job to be completed: %s", jobName));
                        number[0] = item.getExecutable().getNumber();

                    });

            return number[0];


        } catch (Exception e) {
            throw new RuntimeException("No ability to build Jenkins job with name: " + jobName, e);
        }
    }


    public void waitForJobToBeAvailable(String jobName) {
        log.info("Перевірка наявності та очікування Jenkins job-и: " + jobName);
        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())

                .untilAsserted(() -> {
                    Assertions.assertTrue(isJobPresent(jobName),
                            String.format("Waiting for job to be available: %s", jobName));
                });
    }

    public void waitForJobToBeAvailable(String folderName, String jobName) {
        log.info(new ParameterizedMessage("Перевірка наявності та очікування Jenkins job-и: {} в папці: {}", jobName,
                folderName));
        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())

                .untilAsserted(() -> Assertions.assertTrue(isJobPresent(folderName, jobName),
                        String.format("Waiting for job to be available: %s", jobName)));
    }

    @SneakyThrows
    public boolean isJobPresent(String jobName) {
        log.info("Первірка наявності Jenkins job-и " + jobName);
        Set<String> jobs = server.getJobs().keySet();
        return jobs.contains(jobName);
    }

    @SneakyThrows
    public boolean isJobPresent(String folderName, String jobName) {
        log.info(new ParameterizedMessage("Первірка наявності Jenkins job-и {}", jobName));
        var job = server.getJob(folderName);
        Assertions.assertNotNull(job);
        Optional<FolderJob> folder = server.getFolderJob(job);

        return server.getJob(folder.get(), jobName).isBuildable();
    }

    @SneakyThrows
    public void waitJobCompletion(String jobName, long buildId) {
        log.info(new ParameterizedMessage("Очікування завершення роботи Jenkins job {} для збірки {}", jobName,
                buildId));
        log.info("Job started " + jobName);

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .ignoreExceptionsInstanceOf(NullPointerException.class)
                .untilAsserted(() -> {
                    Build build = server.getJob(jobName).getBuildByNumber((int) buildId);
                    Assertions.assertNotNull(build.details().getResult(), String.format("Waiting for job completion: "
                            + "%s", jobName));
                });

        Assertions.assertEquals("SUCCESS", server.getJob(jobName)
                .getLastBuild().details().getResult().name(), String.format("Waiting for job completion as SUCCESS: "
                + "%s", jobName));


        log.info("Job completed " + jobName);
    }

    @SneakyThrows
    public long startJob(String folderName, String jobName, HashMap<String, String> params) {
        log.info("Запуск Jenkins job-и з папки за повернення її номеру");
        try {

            var build = getJobInFolder(folderName, jobName).build(params);

            final long[] number = new long[1];

            await()
                    .pollInterval(1, TimeUnit.SECONDS)
                    .pollInSameThread()
                    .atMost(poolIntervalTimeout, TimeUnit.MINUTES)
                    .untilAsserted(() -> {
                        var item = server.getQueueItem(build);
                        if (item.getWhy() != null)
                            Assertions.assertFalse((item.getWhy().contains("Finished waiting") ||
                                            item.getWhy().contains("is already in progress ")),
                                    String.format("Waiting for job to be completed: %s", jobName));
                        number[0] = item.getExecutable().getNumber();

                    });

            return number[0];


        } catch (Exception e) {
            throw new RuntimeException("No ability to build Jenkins job with name: " + jobName, e);
        }
    }

    public JobWithDetails getJobInFolder(String folderName, String jobName) {
        final JobWithDetails[] jobWithDetails = {null};
        await()
                .alias("Waiting for job in folder availability")
                .pollInterval(2, TimeUnit.SECONDS)
                .pollInSameThread()
                .atMost(5, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    var jobFolder = server.getJob(folderName);
                    Assertions.assertNotNull(jobFolder);
                    Optional<FolderJob> folder = server.getFolderJob(jobFolder);

                    jobWithDetails[0] = server.getJob(folder.get(), jobName);
                    Assertions.assertNotNull(jobFolder);
                });

        return jobWithDetails[0];
    }

    public long startJobInFolder(String folderName, String jobName, HashMap<String, String> params) {
        log.info(new ParameterizedMessage("Запуск Jenkins job-и {} з папки {} за повернення її номеру", jobName,
                folderName));
        try {

            var job = server.getJob(folderName);
            Assertions.assertNotNull(job);
            Optional<FolderJob> folder = server.getFolderJob(job);

            var build = server.getJob(folder.get(), jobName).build(params);

            final long[] number = new long[1];

            await()
                    .pollInterval(1, TimeUnit.SECONDS)
                    .pollInSameThread()
                    .atMost(poolIntervalTimeout, TimeUnit.MINUTES)
                    .untilAsserted(() -> {
                        var item = server.getQueueItem(build);
                        if (item.getWhy() != null)
                            Assertions.assertFalse((item.getWhy().contains("Finished waiting") ||
                                            item.getWhy().contains("is already in progress ")),
                                    String.format("Waiting for job to be completed: %s", jobName));
                        number[0] = item.getExecutable().getNumber();

                    });

            return number[0];


        } catch (Exception e) {
            throw new RuntimeException("No ability to build Jenkins job with name: " + jobName, e);
        }
    }


    @SneakyThrows
    public void waitJobCompletion(String folderName, String jobName, int buildId) {
        log.info(new ParameterizedMessage("Очікування завершення роботи Jenkins job у директорії {}, назва "
                + "job {},номер збірки {}", folderName, jobName, buildId));
        log.info("Job started " + jobName);
        AtomicReference<Build> buildResult = new AtomicReference<>();

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .ignoreExceptionsInstanceOf(NullPointerException.class)
                .untilAsserted(() -> {
                    var job = server.getJob(folderName);
                    Assertions.assertNotNull(job);
                    Optional<FolderJob> folder = server.getFolderJob(job);

                    Build build = server.getJob(folder.get(), jobName).getBuildByNumber(buildId);
                    Assertions.assertNotNull(build.details().getResult(), String.format("Waiting for job completion: "
                            + "%s", jobName));
                    buildResult.set(build);

                });

        Assertions.assertEquals(BuildResult.SUCCESS, buildResult.get().details().getResult(),
                String.format("Job must be with a status of SUCCESS: %s", buildResult.get().getUrl()));
        log.info("Job completed " + jobName);
    }

    @SneakyThrows
    public String getJobUrlByBuildNumber(String jobName, long buildNumber) {
        log.info(new ParameterizedMessage("Отримання URL до jenkins job за {} та {}", jobName, buildNumber));
        return server.getJob(jobName).getBuildByNumber((int) buildNumber).getUrl() + "artifact/excerpt.pdf";
    }

    @SneakyThrows
    public String getJobUrlByBuildNumber(String folderName, String jobName, long buildNumber) {
        log.info(new ParameterizedMessage("Отримання URL до jenkins job за {} та {}", jobName, buildNumber));
        var job = server.getJob(folderName);
        Assertions.assertNotNull(job);
        Optional<FolderJob> folder = server.getFolderJob(job);

        return server.getJob(folder.get(), jobName).getBuildByNumber((int) buildNumber).getUrl() + "artifact/excerpt"
                + ".pdf";
    }

    @SneakyThrows
    public void waitLastJobCompletion(String folderName, String jobName) {
        log.info(new ParameterizedMessage("Очікування завершення роботи Jenkins job у директорії "
                + "{}, назва job {}", folderName, jobName));
        log.info("Job started " + jobName);
        AtomicReference<Build> buildResult = new AtomicReference<>();

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .ignoreExceptionsInstanceOf(NullPointerException.class)
                .untilAsserted(() -> {
                    var folderWithDetails = server.getJob(folderName);
                    Assertions.assertNotNull(folderWithDetails, String.format("Folder %s does not exist", folderName));

                    Optional<FolderJob> folderJob = server.getFolderJob(folderWithDetails);
                    Assertions.assertNotNull(folderJob.orNull(), String.format("Folder %s does not exist", folderName));

                    var job = server.getJob(folderJob.get(), jobName);
                    Assertions.assertNotNull(job,
                            String.format("Job %s does not exist in folder %s", jobName, folderWithDetails.getUrl()));

                    Build build = job.getLastBuild();
                    Assertions.assertNotNull(build.details().getResult(), String.format("Waiting for job completion: "
                            + "%s", jobName));
                    buildResult.set(build);

                });

        Assertions.assertEquals(BuildResult.SUCCESS, buildResult.get().details().getResult(),
                String.format("Job must be with a status of SUCCESS: %s", buildResult.get().getUrl()));
        log.info("Job completed " + jobName);
    }
}

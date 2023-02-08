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

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.LogConfig.logConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.message.ParameterizedMessage;
import com.offbytwo.jenkins.model.QueueReference;

/**
 * Wrap for {@link JenkinsClient} that extends methods of library
 */
@Log4j2
public class JenkinsRestClient extends JenkinsClient {
    private final String inputRequestId;
    private final RequestSpecification requestSpecification;
    private final Service jenkins;

    private static final String INPUT_REQUEST_URL = "job/{jobFolder}/job/{jobName}/{buildNumber}/input/";
    private static final String SCRIPT_REQUEST_URL = "scriptText";

    public JenkinsRestClient(Service jenkins, String inputRequestId) {
        super(jenkins);
        this.jenkins = jenkins;
        this.inputRequestId = inputRequestId;

        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setConfig(
                        config()
                                .logConfig(logConfig()
                                        .enableLoggingOfRequestAndResponseIfValidationFails()
                                        .enablePrettyPrinting(Boolean.TRUE))
                )
                .setBaseUri(jenkins.getUrl());

        requestSpecification = requestSpecBuilder.build();
    }

    @SneakyThrows
    public void startJobWithInputRequest(String jobFolder, String jobName, Map<String, String> params, Map<String,
            String> inputRequest) {
        log.info(new ParameterizedMessage("Запуск Jenkins job з ім'ям {} в папці {}, параметрами "
                + "{} та користувацьким вибором {}", jobName, jobFolder, params, inputRequest));
        waitForJobToBeAvailable(jobFolder, jobName);

        var build = getJobInFolder(jobFolder, jobName).build(params);
        waitJobStarted(build);

        int buildNumber = Math.toIntExact(server.getQueueItem(build).getExecutable().getNumber());

        waitForInputRequest(jobFolder, jobName, buildNumber);
        processUserInputRequest(jobFolder, jobName, buildNumber, inputRequest);

        waitJobCompletion(jobFolder, jobName, buildNumber);
    }

    public void startJobWithInputRequest(String jobFolder, String jobName, Map<String, String> inputRequest) {
        startJobWithInputRequest(jobFolder, jobName, new HashMap<>(), inputRequest);
    }


    private void waitJobStarted(QueueReference build) {
        await()
                .pollInterval(2, TimeUnit.SECONDS)
                .pollInSameThread()
                .atMost(2, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    var executable = server.getQueueItem(build).getExecutable();
                    assertThat(executable).isNotNull();
                });
    }

    private void processUserInputRequest(String jobFolder, String jobName, long buildNumber,
            Map<String, String> inputRequest) {
        given()
                .spec(requestSpecification)
                .auth()
                .preemptive()
                .basic(jenkins.getUser().getLogin(), jenkins.getUser().getPassword())
                .contentType("application/x-www-form-urlencoded; charset=utf-8")
                .formParams(inputRequest)
                .pathParams("jobFolder", jobFolder, "jobName", jobName, "buildNumber", buildNumber)
                .post(INPUT_REQUEST_URL + inputRequestId + "/submit")
                .then()
                .statusCode(302);
    }

    private void waitForInputRequest(String jobFolder, String jobName, long buildNumber) {
        await()
                .pollInterval(2, TimeUnit.SECONDS)
                .pollInSameThread()
                .atMost(5, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    String response = given()
                            .spec(requestSpecification)
                            .auth()
                            .preemptive()
                            .basic(jenkins.getUser().getLogin(), jenkins.getUser().getPassword())
                            .pathParams("jobFolder", jobFolder, "jobName", jobName, "buildNumber", buildNumber)
                            .get(INPUT_REQUEST_URL)
                            .then()
                            .extract()
                            .asString();

                    assertThat(response).contains("Paused for Input :");
                });
    }

    public void executeJenkinsScript(String script){
        given()
                .spec(requestSpecification)
                .contentType("multipart/form-data")
                .auth()
                .preemptive()
                .basic(jenkins.getUser().getLogin(), jenkins.getUser().getPassword())
                .multiPart("script", script)
                .post(SCRIPT_REQUEST_URL)
                .then()
                .statusCode(200);
    }
}

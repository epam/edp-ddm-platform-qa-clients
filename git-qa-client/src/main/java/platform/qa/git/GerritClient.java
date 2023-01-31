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

package platform.qa.git;


import static io.restassured.RestAssured.basic;
import static io.restassured.RestAssured.given;
import static io.restassured.config.ConnectionConfig.connectionConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ConnectionConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Service;
import platform.qa.entities.WaitConfiguration;
import platform.qa.git.entity.Status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import platform.qa.git.entity.changes.ChangesDetailResponse;

/**
 * Client to work with Gerrit create, review, submit changes
 */
@Log4j2
public class GerritClient {
    private static final String CHANGE_ID = "changeId";
    private static final String CHANGE_ENDPOINT = "/a/changes/";
    private static final String PUBLISH_ENDPOINT = "/a/changes/{changeId}/edit:publish";
    private static final String DETAILS_ENDPOINT = "/a/changes/{changeId}/detail";
    private static final String REVIEW_ENDPOINT = "/a/changes/{changeId}/revisions/{revision}/review";
    private static final String SUBMIT_ENDPOINT = "/a/changes/{changeId}/revisions/{revision}/submit";
    private static final String PROJECT_ENDPOINT = "/a/projects/{projectName}";
    private static final String DELETE_ENDPOINT = "/a/changes/{changeId}";
    public static final String BUILD_SUCCESSFUL = "Build Successful";
    public static final String BUILD_FAILED = "Build Failed";

    private final RequestSpecification requestSpec;
    @Setter
    private WaitConfiguration waitConfiguration;

    private String defaultRepositoryName = "registry-regulations";
    private String folder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GerritClient(Service gerrit) {
        waitConfiguration = WaitConfiguration.newBuilder().build();
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder();
        requestSpecBuilder.setConfig(
                        config()
                                .logConfig(logConfig()
                                        .enableLoggingOfRequestAndResponseIfValidationFails()
                                        .enablePrettyPrinting(Boolean.TRUE))
                                .connectionConfig(connectionConfig().closeIdleConnectionsAfterEachResponse()))
                .setBaseUri(gerrit.getUrl())
                .setAuth(basic(gerrit.getLogin(), gerrit.getPassword()))
                .setContentType(ContentType.JSON);

        requestSpec = requestSpecBuilder.build();
    }

    public GerritClient setRepositoryName(String repositoryName) {
        defaultRepositoryName = repositoryName;
        return this;
    }

    public GerritClient setFolderName(String folder) {
        this.folder = folder;
        return this;
    }


    private String createChange() {
        log.info("Створення зміни у герриті");
        Map<String, String> payload = Map.of("project", defaultRepositoryName,
                "subject", RandomStringUtils.randomAlphabetic(7),
                "branch", "master",
                "topic", "test",
                "status", "NEW");

        String response = given()
                .spec(requestSpec)
                .body(payload)
                .post(CHANGE_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .body()
                .asString();

        var changeId = StringUtils.substringBetween(response, "change_id\":\"", "\"");

        assertThat(changeId).withFailMessage("ChangeId is empty: " + response).isNotBlank();
        log.info("Change is created");
        return changeId;
    }

    private void addFileToChange(String changeId, String fileName, String folder) {
        log.info("Додавання файлу до зміни у герриті");
        String file = encodeFileToString("target", fileName);
        Map<String, String> payload = Map.of("binary_content", "data:text/plain;base64," + file);

        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .ignoreExceptions()
                .pollInSameThread()
                .untilAsserted(() -> {
                    int statusCode = given()
                            .spec(requestSpec)
                            .pathParam("f", folder + "/")
                            .body(payload)
                            .when()
                            .put(String.format("/a/changes/%s/edit/{f}%s", changeId, fileName))
                            .then()
                            .extract()
                            .response()
                            .statusCode();
                    ConnectionConfig connectionConfig = new ConnectionConfig();
                    connectionConfig.closeIdleConnectionsAfterEachResponse();
                    assertThat(statusCode).as("Status Code:").isEqualTo(HttpStatus.SC_NO_CONTENT);
                });
        log.info("File " + fileName + " was added to change " + changeId);
    }

    private void publishChangeEdit(String changeId) {
        log.info("Публікація оновленої зміни у герриті");
        given()
                .spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .when()
                .body(Map.of("notify", "NONE"))
                .post(PUBLISH_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
        log.info("Change edit was published.");
    }

    private void verifyJenkinsStatus(String changeId) {
        log.info("Верифікація Jenkins статусу по розгортанню процеса");
        final String[] response = new String[1];
        await()
                .pollInterval(waitConfiguration.getPoolIntervalTimeout(), waitConfiguration.getPoolIntervalTimeUnit())
                .pollInSameThread()
                .atMost(waitConfiguration.getWaitTimeout(), waitConfiguration.getWaitTimeUnit())
                .pollInSameThread()
                .untilAsserted(() -> {
                    response[0] = given()
                            .spec(requestSpec)
                            .pathParam(CHANGE_ID, changeId)
                            .when()
                            .get(DETAILS_ENDPOINT)
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .contentType(ContentType.JSON)
                            .extract()
                            .response()
                            .asString();

                    assertThat(response[0]).as("Jenkins Build didn't set as Success or Fail")
                            .matches(resp -> List.of(BUILD_SUCCESSFUL, BUILD_FAILED).stream().anyMatch(resp::contains));
                });
        String urlFromText = getUrlFromText(response[0]);
        assertThat(response[0]).as("Jenkins Build Failed:").contains(BUILD_SUCCESSFUL);
        log.info("Change verified on Jenkins build: " + urlFromText);
    }

    @SneakyThrows(JsonProcessingException.class)
    public ChangesDetailResponse getChangesDetailById(String changeId) {
        String string = given().spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .when()
                .get(DETAILS_ENDPOINT).then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .extract().body().asString().replace(")]}'", "");

        return objectMapper.readValue(string, ChangesDetailResponse.class);
    }

    private void markChangeAsReviewed(String changeId, int revision) {
        log.info("Рев'ю зміни у герриті");
        Map<String, Object> payload = Map.of("drafts", "PUBLISH_ALL_REVISIONS",
                "labels", Map.of("Code-Review", 2, "Verified", 1, "Sonar-Verified", 1),
                "message", StringUtils.EMPTY,
                "reviewers", Collections.EMPTY_LIST.toArray());

        given()
                .spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .pathParam("revision", revision)
                .when()
                .body(payload)
                .post(REVIEW_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_OK);
        log.info("Change reviewed.");
    }

    private void submitChange(String changeId, int revision) {
        log.info("Затвердження зміни у герриті");
        given()
                .spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .pathParam("revision", revision)
                .when()
                .body("{}")
                .post(SUBMIT_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_OK);
        log.info("Change submitted.");
    }

    public void deployChange(String file) {
        String changeId = createChange();
        addFileToChange(changeId, file, folder);
        publishChangeEdit(changeId);
        markChangeAsReviewed(changeId, 2);
        submitChange(changeId, 2);
        verifyJenkinsStatus(changeId);
    }

    public void reviewAndSubmitRequest(String changeId) {
        markChangeAsReviewed(changeId, 1);
        submitChange(changeId, 1);
        verifyJenkinsStatus(changeId);
    }

    public void reviewRevisionOneAndSubmit(String changeId) {
        markChangeAsReviewed(changeId, 1);
        submitChange(changeId, 1);
    }

    public void deployChange(List<String> files) {
        String changeId = createChange();
        IntStream.range(0, files.size()).forEach(i
                -> addFileToChange(changeId, files.get(i), folder));
        publishChangeEdit(changeId);
        markChangeAsReviewed(changeId, 2);
        submitChange(changeId, 2);
        verifyJenkinsStatus(changeId);
    }

    @SneakyThrows(JsonProcessingException.class)
    public List<Map<String, Object>> getChangeInfo(Status status, int countLastChanges) {
        String json = given()
                .spec(requestSpec)
                .queryParam("q", status.getStatus())
                .queryParam("n", countLastChanges)
                .get(CHANGE_ENDPOINT)
                .body().asString().replace(")]}'", "");

        return new ObjectMapper().readValue(json, new TypeReference<>() {
        });
    }

    @SneakyThrows(JsonProcessingException.class)
    public Map<String, Object> getRepository(String name) {
        String json = given()
                .spec(requestSpec)
                .pathParam("projectName", name)
                .get(PROJECT_ENDPOINT)
                .body().asString().replace(")]}'", "").replaceAll("Not found.*\\n", "{}");

        return new ObjectMapper().readValue(json, new TypeReference<>() {
        });
    }

    private String getUrlFromText(String lineToSearch) {
        String regExp = "(http|https)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*";
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(lineToSearch);
        assertThat(matcher.find()).as("Url was not find").isTrue();
        return matcher.group(0);
    }

    private String encodeFileToString(String folder, String fileName) {
        Path path;
        byte[] bytes;
        try {
            path = Path.of(folder, FilenameUtils.getName(fileName));
            bytes = Files.readAllBytes(path);
        } catch (Exception e) {
            throw new RuntimeException("File does not exist!", e);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    public void markChangeAsVerified(String changeId, int revision, Object verified) {
        log.info("Встановлення значення verified у існуючому запиті з id =" + changeId + " у Gerrit");
        Map<String, Object> payload =
                Map.of(
                        "labels", Map.of("Verified", verified),
                        "message", StringUtils.EMPTY,
                        "reviewers", Collections.EMPTY_LIST.toArray());

        given()
                .spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .pathParam("revision", revision)
                .when()
                .body(payload)
                .post(REVIEW_ENDPOINT)
                .then()
                .statusCode(HttpStatus.SC_OK);
        log.info("Verified status has been changed");
    }

    public String getChangesDetailByIdAsString(String changeId) {
        String response = given().spec(requestSpec)
                .pathParam(CHANGE_ID, changeId)
                .when()
                .get(DETAILS_ENDPOINT).then()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.JSON)
                .extract().body().asString().replace(")]}'", "");
        return response;
    }
}

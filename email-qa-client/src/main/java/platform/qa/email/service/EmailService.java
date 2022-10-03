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

package platform.qa.email.service;

import static io.restassured.RestAssured.given;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.extern.log4j.Log4j2;

/**
 * Service to get mails, read or delete
 */
@Log4j2
public class EmailService {

    public final static String E_MAIL_API_URL = System.getProperty("email.server");
    public final String E_MAIL_API_ROUTE = "api/v1/mailbox/";
    private final RequestSpecification requestSpec;

    public EmailService() {
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
    }

    public ValidatableResponse getAllUserMails(String userName) {
        log.info("GET Mails By User Name");
        return given()
                .spec(requestSpec)
                .when()
                .get(E_MAIL_API_URL + E_MAIL_API_ROUTE + userName)
                .then();
    }

    public ValidatableResponse getUserMailById(String userName, String mailId) {
        log.info("GET Mail info By User Name and User Id");
        return given()
                .spec(requestSpec)
                .when()
                .get(E_MAIL_API_URL + E_MAIL_API_ROUTE + userName + "/" + mailId)
                .then();
    }

    public ValidatableResponse deleteAllUserMails(String userName) {
        log.info("DELETE Mail info By User Name");
        return given()
                .spec(requestSpec)
                .when()
                .delete(E_MAIL_API_URL + E_MAIL_API_ROUTE + userName)
                .then();
    }

    public ValidatableResponse deleteUserMailById(String userName, String mailId) {
        log.info("DELETE Mail info By User Name and mail Id");
        return given()
                .spec(requestSpec)
                .when()
                .delete(E_MAIL_API_URL + E_MAIL_API_ROUTE + userName + "/" + mailId)
                .then();
    }
}

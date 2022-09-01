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

package platform.qa.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
public abstract class BaseServiceClient {

    private final int pollingInterval = 3;
    private final int waitingTimeout = 5;

    @SneakyThrows
    protected Response waitFor(RequestSpecification request, Method method, String url) {
        final Response[] response = {null};
        await()
                .pollInterval(pollingInterval, TimeUnit.SECONDS)
                .atMost(waitingTimeout, TimeUnit.MINUTES)
                .untilAsserted(() -> {
                    response[0] = retrieveResponse(request, method, url);
                    assertThat(503)
                            .as("Service is not ready: " + response[0].body().asString())
                            .isNotEqualTo(response[0].statusCode());

                    assertThat(504)
                            .as("Gateway time out on requests: " + url)
                            .isNotEqualTo(response[0].statusCode());
                });
        return response[0];
    }

    @SneakyThrows
    protected Response retrieveResponse(RequestSpecification request, Method method, String url) {
        log.info("RequestSpecification = " + request);
        log.info("Method = " + method);
        log.info("url = " + url);
        return (url != null)
                ? (Response) request.request(method, new URI(url)).then().extract()
                : (Response) request.request(method).then().extract();
    }

}

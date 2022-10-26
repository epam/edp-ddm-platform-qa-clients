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

package platform.qa.rest.client.impl;

import io.restassured.http.ContentType;
import platform.qa.entities.Service;

/**
 * Preparation of requests by {@link platform.qa.rest.client.RestClient} that allows
 * to perform negative and positive requests.
 *
 * Example of usage:
 * <code>
 *     new RestClientProxy(service)
 *             .get(
 *                  "hello/world",
 *                  null,
 *                  new TypeReference<List<String>>() {}.getType(),
 *                  HttpStatus.SC_OK
 *              );
 * </code>
 */
public class RestClientProxy {
    private Service service;

    public RestClientProxy(Service service) {
        this.service = service;
    }

    /**
     * Preparation for positive HTTP request
     * @return {@link RestClientImpl} that ready for positive HTTP request
     */
    public RestClientImpl positiveRequest() {
        return new RestClientImpl(service.getUrl(), service.getUser().getToken());
    }

    /**
     * Preparation for positive HTTP requests with specified content type
     * @param contentType content type for request
     * @return {@link RestClientImpl} that ready for positive HTTP request
     */
    public RestClientImpl positiveRequest(ContentType contentType) {
        return new RestClientImpl(service.getUrl(), service.getUser().getToken(), contentType);
    }

    /**
     * Preparation for negative HTTP request
     * @return {@link RestClientImpl} that ready for negative HTTP request
     */
    public RestClientImpl negativeRequest() {
        return new RestClientImpl(service.getUrl(), null);
    }

    /**
     * Preparation for negative HTTP requests with specified content type
     * @param contentType content type for request
     * @return {@link RestClientImpl} that ready for negative HTTP request
     */
    public RestClientImpl negativeRequest(ContentType contentType) {
        return new RestClientImpl(service.getUrl(), null, contentType);
    }
}

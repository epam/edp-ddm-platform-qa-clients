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

package platform.qa.keycloak;

import lombok.extern.log4j.Log4j2;
import org.jboss.resteasy.util.ReadFromStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;

/**
 * Keycloak logger
 */
@Log4j2
public class KeycloakLoggingFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        if (Response.Status.Family.SERVER_ERROR.equals(Response.Status.fromStatusCode(responseContext.getStatus()).getFamily())) {
            log.error(getRequest(requestContext));
            log.error(getResponse(responseContext));
        } else {
            log.debug(getRequest(requestContext));
            log.debug(getResponse(responseContext));
        }
    }

    private String getRequest(ClientRequestContext requestContext) {
        return String.format("\nRequest URI: %s\n" +
                        "Request method: %s\n" +
                        "Request headers: %s\n" +
                        "Request cookies: %s\n" +
                        "Request entity: %s\n",
                requestContext.getUri(),
                requestContext.getMethod(),
                requestContext.getHeaders(),
                requestContext.getCookies(),
                requestContext.getEntity());
    }

    private String getResponse(ClientResponseContext responseContext) {
        return String.format("\nResponse code: %s\n"
                        + "Response message: %s\n"
                        + "Response date: %s\n"
                        + "Response entity: %s\n",
                responseContext.getStatus(),
                responseContext.getStatusInfo() != null ?
                        responseContext.getStatusInfo().getReasonPhrase() : responseContext.getStatusInfo(),
                responseContext.getDate(),
                responseContext.hasEntity() ? getEntity(responseContext) : null);
    }


    private String getEntity(ClientResponseContext responseContext) {
        byte[] cached;
        try {
            cached = ReadFromStream.readFromStream(1024, responseContext.getEntityStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String body = new String(cached, StandardCharsets.UTF_8);

        InputStream targetStream = new ByteArrayInputStream(cached);
        responseContext.setEntityStream(targetStream);
        return body;
    }

}

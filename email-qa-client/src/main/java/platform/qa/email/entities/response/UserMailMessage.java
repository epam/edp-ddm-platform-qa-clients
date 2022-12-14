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

package platform.qa.email.entities.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMailMessage {

    @JsonProperty("posix-millis")
    private long posixMillis;
    private String date;
    private String mailbox;
    private List<Object> attachments;
    private int size;
    private String subject;
    private Header header;
    private String from;
    private String id;
    private List<String> to;
    private Body body;
    private boolean seen;

    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private String html;
        private String text;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {

        @JsonProperty("Date")
        private List<String> date;

        @JsonProperty("Dkim-Signature")
        private List<String> dKimSignature;

        @JsonProperty("From")
        private List<String> from;

        @JsonProperty("Message-Id")
        private List<String> messageId;

        @JsonProperty("Received")
        private List<String> received;

        @JsonProperty("Subject")
        private List<String> subject;

        @JsonProperty("To")
        private List<String> to;
    }
}
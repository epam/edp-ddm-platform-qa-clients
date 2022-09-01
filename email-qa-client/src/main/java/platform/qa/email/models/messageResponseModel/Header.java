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

package platform.qa.email.models.messageResponseModel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@NoArgsConstructor
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class Header{

	@JsonProperty("Received")
	private List<String> received;

	@JsonProperty("X-Google-Smtp-Source")
	private List<String> xGoogleSmtpSource;

	@JsonProperty("From")
	private List<String> from;

	@JsonProperty("Message-Id")
	private List<String> messageId;

	@JsonProperty("In-Reply-To")
	private List<String> inReplyTo;

	@JsonProperty("Date")
	private List<String> date;

	@JsonProperty("Subject")
	private List<String> subject;

	@JsonProperty("Dkim-Signature")
	private List<String> dkimSignature;

	@JsonProperty("References")
	private List<String> references;

	@JsonProperty("X-Google-Dkim-Signature")
	private List<String> xGoogleDkimSignature;

	@JsonProperty("X-Received")
	private List<String> xReceived;

	@JsonProperty("To")
	private List<String> to;

	@JsonProperty("Mime-Version")
	private List<String> mimeVersion;

	@JsonProperty("Content-Type")
	private List<String> contentType;

	@JsonProperty("X-Gm-Message-State")
	private List<String> xGmMessageState;
}
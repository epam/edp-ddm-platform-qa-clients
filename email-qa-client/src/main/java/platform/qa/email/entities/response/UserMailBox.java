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
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Accessors(chain = true)
public class UserMailBox implements Comparable<UserMailBox>{

	@JsonProperty("posix-millis")
	private long posixMillis;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private Date date;
	private String mailbox;
	private int size;
	private String subject;
	private String from;
	private String id;
	private List<String> to;
	private boolean seen;

	@Override
	public int compareTo(UserMailBox otherUserMail) {
		return getDate().compareTo(otherUserMail.getDate());
	}
}
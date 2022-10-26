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

package platform.qa.git.gerrit.service.api;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import platform.qa.entities.Repository;
import platform.qa.git.gerrit.dto.FileDateDto;

import java.util.List;
import java.util.Map;

/**
 * Facade for two Gerrit clients.
 */
public interface GerritFacade {

    /**
     * Clone git repository
     * @param repository repository representation {@link Repository}
     * @param path path to folder to save repository
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    Git cloneRepository(Repository repository, String path) throws GitAPIException;

    /**
     * Fetch repository on remote
     * @param git {@link Git} repository
     * @param refs remote refs
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    Git fetchRepository(Git git, String refs) throws GitAPIException;

    /**
     * Pull repository
     * @param git {@link Git} repository
     * @param head repository head
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    Git pullRepository(Git git, String head) throws GitAPIException;

    /**
     * Get file init and last modified dates
     * @param git {@link Git} repository
     * @param path path for file
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    FileDateDto getFirstAndLastCommit(Git git, String path) throws GitAPIException;

    /**
     * Get commit difference for change request
     * @param changeId id of change in Gerrit
     * @return {@link java.util.HashMap} that has key with file name and value {@link FileInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    Map<String, FileInfo> getChanges(String changeId) throws RestApiException;

    /**
     * Get list of changes by gerrit query
     * @param query gerrit query
     * @return {@link java.util.ArrayList} with {@link ChangeInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    List<ChangeInfo> getChangesByQuery(String query) throws RestApiException;

    /**
     * Create Gerrit change
     * @param change {@link ChangeInput}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    void createChange(ChangeInput change) throws RestApiException;

    /**
     * Get change information by Gerrit change id
     * @param changeId Gerrit change id
     * @return {@link ChangeInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    ChangeInfo getChangeInfo(String changeId) throws RestApiException;

    /**
     * Delete Gerrit change by change id
     * @param changeId Gerrit change id
     * @throws RestApiException throws when possible problems with Gerrit
     */
    void deleteChange(String changeId) throws RestApiException;
}

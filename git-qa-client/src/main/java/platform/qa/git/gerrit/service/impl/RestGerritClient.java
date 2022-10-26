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

package platform.qa.git.gerrit.service.impl;

import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;
import platform.qa.entities.Service;

import java.util.List;
import java.util.Map;

/**
 * Rest Gerrit client implementation
 */
public class RestGerritClient {
    private final GerritApi gerrit;

    public RestGerritClient(Service gerritService) {
        GerritAuthData.Basic gerritAuth = new GerritAuthData.Basic(
                gerritService.getUrl(),
                gerritService.getUser().getLogin(),
                gerritService.getUser().getPassword()
        );

        gerrit = new GerritRestApiFactory().create(gerritAuth);
    }

    /**
     * Get list of changes by gerrit query
     * @param query gerrit query
     * @return {@link java.util.ArrayList} with {@link ChangeInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    public List<ChangeInfo> getChangesByQuery(String query) throws RestApiException {
        return gerrit.changes().query(query).get();
    }

    /**
     * Create Gerrit change
     * @param change {@link ChangeInput}
     * @return {@link ChangeApi}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    public ChangeApi createChange(ChangeInput change) throws RestApiException {
        return gerrit.changes().create(change);
    }

    /**
     * Get change information by Gerrit change id
     * @param changeId Gerrit change id
     * @return {@link ChangeInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    public ChangeInfo getChangeInfo(String changeId) throws RestApiException {
        return gerrit.changes().id(changeId).get();
    }

    /**
     * Get commit difference for change request
     * @param changeId id of change in Gerrit
     * @return {@link java.util.HashMap} that has key with file name and value {@link FileInfo}
     * @throws RestApiException throws when possible problems with Gerrit
     */
    public Map<String, FileInfo> getFileChangesFromChange(String changeId) throws RestApiException {
        return gerrit.changes().id(changeId).current().files();
    }

    /**
     * Delete Gerrit change by change id
     * @param changeId Gerrit change id
     * @throws RestApiException throws when possible problems with Gerrit
     */
    public void deleteChange(String changeId) throws RestApiException {
        gerrit.changes().id(changeId).delete();
    }

}

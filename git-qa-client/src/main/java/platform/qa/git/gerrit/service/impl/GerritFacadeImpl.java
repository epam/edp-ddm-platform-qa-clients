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

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.ChangeInput;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import platform.qa.entities.Repository;
import platform.qa.entities.Service;
import platform.qa.git.gerrit.dto.FileDateDto;
import platform.qa.git.gerrit.service.api.GerritFacade;

import java.util.List;
import java.util.Map;

public class GerritFacadeImpl implements GerritFacade {
    private final JGitGerritClient jgit;
    private final RestGerritClient gerrit;

    public GerritFacadeImpl(Service gerrit) {
        jgit = new JGitGerritClient(gerrit);
        this.gerrit = new RestGerritClient(gerrit);
    }

    @Override
    public Git cloneRepository(Repository repository, String path) throws GitAPIException {
        return jgit.cloneRepository(repository, path);
    }

    @Override
    public Git fetchRepository(Git git, String refs) throws GitAPIException {
        return jgit.fetchRepository(git, refs);
    }

    @Override
    public Git pullRepository(Git git, String head) throws GitAPIException {
        return jgit.pullRepository(git, head);
    }

    @Override
    public FileDateDto getFirstAndLastCommit(Git git, String path) throws GitAPIException {
        return jgit.log(git, path);
    }

    @Override
    public Map<String, FileInfo> getChanges(String changeId) throws RestApiException {
        return gerrit.getFileChangesFromChange(changeId);
    }

    @Override
    public List<ChangeInfo> getChangesByQuery(String query) throws RestApiException {
        return gerrit.getChangesByQuery(query);
    }

    @Override
    public void createChange(ChangeInput change) throws RestApiException {
        gerrit.createChange(change);
    }

    @Override
    public ChangeInfo getChangeInfo(String changeId) throws RestApiException {
        return gerrit.getChangeInfo(changeId);
    }

    @Override
    public void deleteChange(String changeId) throws RestApiException {
        gerrit.deleteChange(changeId);
    }
}

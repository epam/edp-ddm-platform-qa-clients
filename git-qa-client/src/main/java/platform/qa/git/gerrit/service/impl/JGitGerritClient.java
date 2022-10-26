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

import com.github.javafaker.Faker;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import platform.qa.entities.Repository;
import platform.qa.entities.Service;
import platform.qa.git.gerrit.dto.FileDateDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * JGit Gerrit client implementation
 */
public class JGitGerritClient {
    private final static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private final CredentialsProvider credentials;

    public JGitGerritClient(Service gerrit) {
        credentials = new UsernamePasswordCredentialsProvider(gerrit.getLogin(), gerrit.getPassword());
    }

    /**
     * Clone git repository
     * @param repository repository representation {@link Repository}
     * @param path path to folder to save repository
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    public Git cloneRepository(Repository repository, String path) throws GitAPIException {
        String clonedDirectoryPath = path + "/" + repository.getRepoName() + "_" + new Faker().numerify("#");

        var g =  Git
                .cloneRepository()
                .setCredentialsProvider(credentials)
                .setURI(repository.getUrl().concat(repository.getRepoName()))
                .setBranch(repository.getBranch())
                .setDirectory(createTemporaryFolder(clonedDirectoryPath))
                .call();


        return g;
    }

    /**
     * Fetch repository on remote
     * @param git {@link Git} repository
     * @param refs remote refs
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    public Git fetchRepository(Git git, String refs) throws GitAPIException {
        git
                .fetch()
                .setCredentialsProvider(credentials)
                .setRefSpecs(refs)
                .call();

        git.checkout().setName("FETCH_HEAD").call();
        return git;
    }

    /**
     * Get file init and last modified dates
     * @param git {@link Git} repository
     * @param path path for file
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    public FileDateDto log(Git git, String path) throws GitAPIException {
        LogCommand log = git.log();
        log.addPath(path);

        var commits = log.call();
        var commitIterator = commits.iterator();

        RevCommit firstCommit = commitIterator.next();
        RevCommit lastCommit = commitIterator.next();

        while (commitIterator.hasNext()) {
            firstCommit = commitIterator.next();
        }

        var created = firstCommit != null ? convertDate(firstCommit.getCommitTime()) : convertDate(System.currentTimeMillis());
        var updated = lastCommit != null ? convertDate(lastCommit.getCommitTime()) : created;

        return FileDateDto
                .builder()
                .created(created)
                .updated(updated)
                .build();
    }

    /**
     * Pull repository
     * @param git {@link Git} repository
     * @param head repository head
     * @return {@link Git}
     * @throws GitAPIException throws when possible problems with Gerrit
     */
    public Git pullRepository(Git git, String head) throws GitAPIException {
        git
                .checkout()
                .setName("refs/heads/" + head)
                .call();

        git
                .pull()
                .setCredentialsProvider(credentials)
                .setRebase(true)
                .call();

        return git;
    }

    private File createTemporaryFolder(String pathName) {
        try {
            Path path = Path.of(pathName);
            File file = path.toFile();
            FileUtils.deleteDirectory(file);
            Files.createDirectory(path);
            return path.toFile();
        } catch (IOException e) {
            throw new RuntimeException("Impossible to create the folder:" + pathName, e);
        }
    }

    private String convertDate(long commitTime) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DATE_PATTERN);
        LocalDateTime date = LocalDateTime.ofEpochSecond(commitTime, 0, ZoneOffset.UTC);
        return dateFormat.format(date);
    }
}

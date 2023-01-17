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

package platform.qa.git;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import platform.qa.entities.Repository;
import platform.qa.entities.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.StringUtils;
import com.github.javafaker.Faker;
import com.google.common.collect.Lists;

/**
 * Client to work with Git create, review, submit changes
 */
@Log4j2
public class JgitClient {
    private Repository gerritToUploadData;
    private Repository gerritInitialData;
    private List<String> foldersToCopy;
    private List<File> filesToUpload;
    private String destinationFolder;

    @Getter
    private Service gerrit;

    public JgitClient(Service gerritToUploadData) {
        this.gerrit = gerritToUploadData;
        this.gerritToUploadData = new Repository(gerritToUploadData, "registry-regulations", "master");
    }

    public JgitClient(Repository gerritToUploadData) {
        this.gerritToUploadData = gerritToUploadData;
    }

    public JgitClient takeTestDataFrom(Repository gitBud, List<String> foldersToCopy) {
        this.gerritInitialData = gitBud;
        this.foldersToCopy = foldersToCopy;
        return this;
    }

    @SneakyThrows
    private Git cloneGerritRepositoryWithHooks(Repository repository, String path) {
        Git repo = cloneRepoLocally(repository, "target");

        Path pathHooks = Path.of(path, repository.getRepoName(), ".git/hooks", FilenameUtils.getName("commit-msg"));
        FileUtils.copyURLToFile(
                new URL(repository.getUrl() + "tools/hooks/commit-msg"),
                pathHooks.toFile(),
                10000,
                5000);

        FS.DETECTED.setExecute(pathHooks.toFile(), true);
        return Git.wrap(repo.getRepository());
    }

    public Git cloneGerritRepositoryWithHooksLocally() {
        return cloneGerritRepositoryWithHooks(gerritToUploadData, "target");
    }

    public Git cloneGerritRepositoryLocally() {
        return cloneRepoLocally(gerritInitialData, "target");
    }

    public String submitChange(Git gerritLocal, Git gerritTestDataLocal) {
        copyFilesToRepository(gerritTestDataLocal, gerritLocal);

        String changeId = commitAndPush(gerritLocal, gerritToUploadData,
                "Committed content: " + StringUtils.join(foldersToCopy, ","));
        gerritLocal.close();
        gerritTestDataLocal.close();

        return changeId;
    }

    public String submitChange() {
        Git gerritLocal = cloneGerritRepositoryWithHooks(gerritToUploadData, "target");
        Git gerritTestDataLocal = cloneRepoLocally(gerritInitialData, "target");
        return submitChange(gerritLocal, gerritTestDataLocal);
    }

    private void copyFilesToRepository(Git source, Git target) {
        if (foldersToCopy.isEmpty()) {
            copyAllFilesFromLocalRepository(source, target);
        } else {
            copyFolderContent(source, target, foldersToCopy);
        }
    }

    public Git cloneRepoLocally(Repository repository, String path) {
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repository.getTokenName(),
                repository.getToken());
        try {
            Git project = Git
                    .cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repository.getUrl() + repository.getRepoName())
                    .setBranch("refs/heads/" + repository.getBranch())
                    .setDirectory(createTmpFolder(path + "/" +
                            repository.getRepoName() + "_" + new Faker().numerify("#####")))
                    .call();

            return repository.getTag() == null ? project : checkoutOnTag(project, repository.getTag());
        } catch (GitAPIException e) {
            throw new RuntimeException("Repository was not cloned!", e);
        }
    }

    public Git cloneRepo() {
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gerrit.getUser().getLogin(),
                gerrit.getUser().getPassword());

        try {
            return Git
                    .cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(gerrit.getUrl() + "/registry-regulations")
                    .setBranch("refs/heads/master")
                    .setDirectory(createTmpFolder("target" + "/" +
                            "registry-regulations" + "_" + new Faker().numerify("#####")))
                    .call();
        } catch (GitAPIException e) {
            throw new RuntimeException("Repository was not cloned!", e);
        }
    }


    public List<String> getRepositoryFolders() {
        Git git = cloneRepoLocally(gerritToUploadData, "target");
        git.close();
        return Arrays.asList(Objects.requireNonNull(git.getRepository().getWorkTree().list()));
    }

    public List getFilesFromFolder(String folder) {
        log.info("Отримання переліку файлів у заданій директорії та головному репозитарії Дати");
        Git git = cloneRepoLocally(gerritToUploadData, "target");
        git.close();
        return Arrays.stream(Objects.requireNonNull(git.getRepository().getWorkTree().listFiles()))
                .filter(a -> folder.equals(a.getName()))
                .map(a -> Arrays.asList(a.listFiles()))
                .collect(Collectors.toList());
    }

    public List getRepositoryContent() {
        log.info("Отримання переліку файлів у вказаній директорії");
        Git git = cloneRepoLocally(gerritToUploadData, "target");
        var array = git.getRepository().getWorkTree().listFiles();
        var list = Lists.newArrayList();
        for (var item : array) {
            list.add(item.getName());
        }

        return list;
    }

    public Git cloneRepositoryAndCheckout(Repository repository, String path) {
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(repository.getTokenName(),
                repository.getToken());
        try {
            Git project = Git
                    .cloneRepository()
                    .setCredentialsProvider(cp)
                    .setURI(repository.getUrl() + repository.getRepoName())
                    .setBranch("refs/heads/" + repository.getBranch())
                    .setDirectory(createTmpFolder(path + "/" +
                            repository.getRepoName() + "_" + new Faker().numerify("#####")))
                    .call();

            return repository.getTag() == null ? project : checkoutOnTag(project, repository.getTag());

        } catch (GitAPIException e) {
            throw new RuntimeException("Repository was not cloned!", e);
        }
    }

    @SneakyThrows
    private String commitAndPush(Git repository, Repository gerrit, String commitMessage) {
        String changeId;
        repository
                .add()
                .addFilepattern(".")
                .call();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        RevCommit commit = repository.commit()
                .setInsertChangeId(true)
                .setMessage(commitMessage)
                .setHookOutputStream("commit-msg", new PrintStream(out))
                .call();
        changeId = commit.getFooterLines("Change-Id").get(0);
        log.info("Commit changeId: " + commit.getFooterLines("Change-Id").get(0));

        repository
                .push()
                .setRemote("origin")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(gerrit.getTokenName(),
                        gerrit.getToken()))
                .setRefSpecs(new RefSpec("HEAD:refs/for/master"))
                .call();

        return changeId;

    }

    private File createTmpFolder(String pathName) {
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

    public Git checkoutOnTag(Git project, String tag) throws GitAPIException {
        project
                .checkout()
                .setCreateBranch(true)
                .setName(tag)
                .setStartPoint("refs/tags/".concat(tag))
                .call();

        return project;
    }

    public JgitClient replaceDataInFolder(Git targetGit, String folderPath, String originalText,
            String replacementText) {
        File[] filesDataFolder = Path.of(targetGit.getRepository().getWorkTree().getAbsolutePath(),
                FilenameUtils.getName(folderPath)).toFile().listFiles();
        Arrays.stream(Objects.requireNonNull(filesDataFolder)).forEach(
                file -> {

                    List<String> lines;
                    try {
                        if (file.isDirectory()) return;

                        lines = FileUtils.readLines(file, Charset.defaultCharset());
                        List<String> replaced = lines.stream()
                                .map(line -> line.replace(originalText, replacementText))
                                .collect(Collectors.toList());

                        FileUtils.writeLines(file, replaced);
                    } catch (IOException e) {
                        throw new RuntimeException("Text " + originalText + " was not replaced on " + replacementText + " "
                                + "inside folder: " + folderPath);
                    }
                }

        );
        return this;
    }

    public JgitClient removeFoldersFromLocalRepository(Git targetGit, List<String> folderPaths) {
        if (folderPaths != null && !folderPaths.isEmpty())
            folderPaths.forEach(path -> {
                File file = Path.of(targetGit.getRepository().getWorkTree().getAbsolutePath(),
                        FilenameUtils.getName(path)).toFile();
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    throw new RuntimeException(path + " folder was not deleted");
                }
            });
        return this;
    }

    private void copyFolderContent(Git sourceGit, Git targetGit, List<String> foldersToCopy) {
        foldersToCopy.forEach(item ->
                {
                    try {
                        File sourceFilesOrFolder = Path.of(sourceGit.getRepository().getWorkTree().getAbsolutePath(),
                                FilenameUtils.getName(item)).toFile();

                        if (sourceFilesOrFolder.isDirectory()) {
                            FileUtils.copyDirectory(
                                    sourceFilesOrFolder,
                                    Path.of(targetGit.getRepository().getWorkTree().getAbsolutePath(),
                                            FilenameUtils.getName(item)).toFile());
                        } else {
                            FileUtils.copyFile(sourceFilesOrFolder,
                                    Path.of(targetGit.getRepository().getWorkTree().getAbsolutePath(),
                                            FilenameUtils.getName(item)).toFile());
                        }

                    } catch (IOException e) {
                        throw new RuntimeException("Impossible to copy the folders etc.", e);
                    }
                }
        );
    }


    private void copyAllFilesFromLocalRepository(Git sourceGit, Git targetGit) {
        try {
            File sourceDirectory = Path.of(sourceGit.getRepository().getWorkTree().getAbsolutePath()).toFile();
            File targetDirectory = Path.of(targetGit.getRepository().getWorkTree().getAbsolutePath()).toFile();


            FileFilter filter = FileFilterUtils
                    .notFileFilter(FileFilterUtils.suffixFileFilter(".git", IOCase.SYSTEM))
                    .and(FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(".md", IOCase.SYSTEM)))
                    .and(FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(".groovy", IOCase.SYSTEM)));

            FileUtils.copyDirectory(sourceDirectory, targetDirectory, filter);
        } catch (IOException e) {
            throw new RuntimeException("Files wasn't copied successful!", e);
        }
    }

    @SneakyThrows
    private List<String> getWorkTreeFiles(Git source) {
        File[] files = source.getRepository().getWorkTree().listFiles();
        String prefix = source.getRepository().getDirectory().getPath().replace(".git", "");

        List<String> result = new ArrayList<>();

        for (File file : files) {
            String fileName = file.getPath().replace(prefix, "");

            if (!fileName.contains(".groovy")) {
                result.add(fileName);
            }
        }

        return result;
    }

}

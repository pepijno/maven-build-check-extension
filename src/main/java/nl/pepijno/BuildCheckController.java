/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.pepijno;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SessionScoped
@Named
public class BuildCheckController {

    private static final Logger LOG = LoggerFactory.getLogger(BuildCheckController.class);
    private static final String FIND_CMD = "/usr/bin/find";

    private final LifecyclePhasesHelper lifecyclePhasesHelper;

    @Inject
    public BuildCheckController(final LifecyclePhasesHelper lifecyclePhasesHelper) {
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
    }

    boolean shouldRebuild(final MavenSession session, final List<MojoExecution> mojoExecutions) {
        final var highestPhase = lifecyclePhasesHelper.resolveHighestLifecyclePhase(mojoExecutions);

        if (!lifecyclePhasesHelper.isLaterPhaseThanClean(highestPhase)) {
            return true;
        }

        final var project = session.getCurrentProject();
        var cacheFile = Utils.getCacheFile(session);
        if (!Files.exists(cacheFile)) {
            LOG.debug("Cache file {} not found", cacheFile);
            return true;
        }

        try (var fis = new FileInputStream(cacheFile.toFile());
                var oos = new ObjectInputStream(fis)) {
            boolean hasResults = findCommandHasResults(findNewerInSourceCommandString(project, cacheFile));
            if (hasResults) {
                LOG.debug("Found newer file in src of project {}", project);
                return true;
            }
            hasResults = findCommandHasResults(findNewerInRootCommandString(project, cacheFile));
            if (hasResults) {
                LOG.debug("Found newer file in root of project {}", project);
                return true;
            }
            var savedFiles = new HashSet<String>();
            savedFiles = (HashSet<String>) oos.readObject();
            var files = findFiles(findInSourceCommandString(project));
            files.addAll(findFiles(findInRootCommandString(project)));
            if (!files.equals(savedFiles)) {
                LOG.debug("Current files in project {} do not match saved files", project);
                return true;
            } else {
                return false;
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            LOG.warn("Could not read project files for project {}", project);
            LOG.debug(e.getMessage());
            return true;
        }
    }

    void save(final MavenSession session) {
        final var project = session.getCurrentProject();
        var projectFilesFilename = Utils.getCacheFile(session);
        try (var fos = new FileOutputStream(projectFilesFilename.toFile());
                var oos = new ObjectOutputStream(fos)) {
            var files = findFiles(findInSourceCommandString(project));
            files.addAll(findFiles(findInRootCommandString(project)));
            LOG.info("Writing project files for project {}", project);
            oos.writeObject(files);
        } catch (IOException | InterruptedException e) {
            LOG.warn("Could not save project files for project {}", project);
            LOG.debug(e.getMessage());
        }
    }

    void removeDownstreamCacheFiles(final MavenSession session, final MavenProject project) {
        for (var downstreamProject : session.getProjectDependencyGraph().getDownstreamProjects(project, false)) {
            removeCacheFile(session, downstreamProject);
        }
    }

    void removeCacheFile(final MavenSession session, final MavenProject project) {
        try {
            var location = Utils.getLocation(session, project);
            var prefix = Utils.getCacheFilenamePrefix(project);
            if (Files.isDirectory(location)) {
                for (File file : location.toFile().listFiles()) {
                    if (file.getName().startsWith(prefix)) {
                        Files.deleteIfExists(file.toPath());
                    }
                }
            }
        } catch (IOException e) {
            LOG.warn("Could not remove cache file for project {}", project);
        }
    }

    private boolean findCommandHasResults(final String findCommand) throws IOException, InterruptedException {
        return !findFiles(findCommand).isEmpty();
    }

    private Set<String> findFiles(final String findCommand) throws IOException, InterruptedException {
        var mdfind = new ProcessBuilder("/bin/bash", "-c", findCommand);

        var process = mdfind.start();

        var files = new HashSet<String>();
        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            files.add(line);
        }
        process.waitFor();
        return files;
    }

    private String findInSourceCommandString(final MavenProject project) {
        return FIND_CMD + " " + project.getBasedir().toPath().resolve("src") + " -type f";
    }

    private String findNewerInSourceCommandString(final MavenProject project, final Path referenceFile) {
        return findInSourceCommandString(project) + " -newer " + referenceFile.toString();
    }

    private String findInRootCommandString(final MavenProject project) {
        return FIND_CMD + " " + project.getBasedir() + " -maxdepth 1 -type f -not -name \".*\"";
    }

    private String findNewerInRootCommandString(final MavenProject project, final Path referenceFile) {
        return findInRootCommandString(project) + " -newer " + referenceFile.toString();
    }
}

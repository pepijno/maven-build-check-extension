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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public BuildCheckController(LifecyclePhasesHelper lifecyclePhasesHelper) {
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
    }

    boolean shouldRebuild(MavenSession session, MavenProject project, List<MojoExecution> mojoExecutions) {
        final String highestPhase = lifecyclePhasesHelper.resolveHighestLifecyclePhase(mojoExecutions);

        if (!lifecyclePhasesHelper.isLaterPhaseThanClean(highestPhase)) {
            return true;
        }

        var cacheFile = Utils.getCacheFile(session, project);
        if (!Files.exists(Paths.get(cacheFile))) {
            LOG.debug("Cache file {} not found", cacheFile);
            return true;
        }

        try (FileInputStream fis = new FileInputStream(cacheFile);
                ObjectInputStream oos = new ObjectInputStream(fis)) {
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
            Set<String> files = findFiles(findInSourceCommandString(project));
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

    void save(final MavenSession session, final MavenProject project) {
        var projectFilesFilename = Utils.getCacheFile(session, project);
        try (FileOutputStream fos = new FileOutputStream(projectFilesFilename);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            Set<String> files = findFiles(findInSourceCommandString(project));
            files.addAll(findFiles(findInRootCommandString(project)));
            LOG.info("Writing project files for project {}", project);
            oos.writeObject(files);
        } catch (IOException | InterruptedException e) {
            LOG.warn("Could not save project files for project {}", project);
            LOG.debug(e.getMessage());
        }
    }

    void removeCacheFiles(final MavenSession session, final MavenProject project) {
        for (MavenProject downstreamProject :
                session.getProjectDependencyGraph().getDownstreamProjects(project, true)) {
            try {
                var projectFilesFilename = Utils.getCacheFile(session, downstreamProject);
                Files.deleteIfExists(Path.of(projectFilesFilename));
            } catch (IOException e) {
                LOG.warn("Could not remove cache file for project {}", project);
            }
        }
    }

    private boolean findCommandHasResults(String findCommand) throws IOException, InterruptedException {
        return !findFiles(findCommand).isEmpty();
    }

    private Set<String> findFiles(String findCommand) throws IOException, InterruptedException {
        ProcessBuilder mdfind = new ProcessBuilder("/bin/bash", "-c", findCommand);

        Process process = mdfind.start();

        Set<String> files = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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

    private String findNewerInSourceCommandString(final MavenProject project, final String referenceFile) {
        return findInSourceCommandString(project) + " -newer " + referenceFile;
    }

    private String findInRootCommandString(final MavenProject project) {
        return FIND_CMD + " " + project.getBasedir() + " -maxdepth 1 -type f -not -name \".*\"";
    }

    private String findNewerInRootCommandString(final MavenProject project, final String referenceFile) {
        return findInRootCommandString(project) + " -newer " + referenceFile;
    }
}

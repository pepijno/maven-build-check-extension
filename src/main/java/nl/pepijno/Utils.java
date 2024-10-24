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

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

class Utils {

    private Utils() {}

    static <T> T getLast(List<T> list) {
        int size = list.size();
        if (size > 0) {
            return list.get(size - 1);
        }
        throw new NoSuchElementException();
    }

    static Path getCacheFile(final MavenSession session) {
        return getCacheFile(session, session.getCurrentProject());
    }

    static Path getCacheFile(final MavenSession session, final MavenProject project) {
        var path = getLocation(session, project);
        var profiles = getSortedProfilesAsString(session);
        return path.resolve(getCacheFilenamePrefix(project) + "-" + profiles + ".files");
    }

    static String getCacheFilenamePrefix(final MavenProject project) {
        return project.getArtifactId() + "-" + project.getVersion();
    }

    static Path getLocation(final MavenSession session, final MavenProject project) {
        return getLocalRepository(session)
                .resolve(project.getGroupId().replace('.', '/'))
                .resolve(project.getArtifactId())
                .resolve(project.getVersion());
    }

    private static Path getLocalRepository(final MavenSession session) {
        return Path.of(session.getLocalRepository().getBasedir());
    }

    private static String getSortedProfilesAsString(final MavenSession session) {
        return session.getRequest().getActiveProfiles().stream().sorted().collect(Collectors.joining("-"));
    }
}

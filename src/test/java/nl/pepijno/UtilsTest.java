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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilsTest {

    private MavenSession session;

    @BeforeEach
    void setUp() {
        ArtifactRepository localRepository = mock(ArtifactRepository.class);
        when(localRepository.getBasedir()).thenReturn("/a/b/c");
        MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("group.id");
        when(project.getArtifactId()).thenReturn("artifact");
        when(project.getVersion()).thenReturn("version");
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        when(request.getActiveProfiles()).thenReturn(List.of("profile1", "profile2"));

        session = mock(MavenSession.class);
        when(session.getLocalRepository()).thenReturn(localRepository);
        when(session.getCurrentProject()).thenReturn(project);
        when(session.getRequest()).thenReturn(request);
    }

    @Test
    void getLast_shouldReturnLastElement_ifListIsNonEmpty() {
        var list = new ArrayList<Integer>();
        list.add(1);
        assertThat(Utils.getLast(list)).isEqualTo(1);
        list.add(2);
        assertThat(Utils.getLast(list)).isEqualTo(2);
        list.add(3);
        assertThat(Utils.getLast(list)).isEqualTo(3);
    }

    @Test
    void getLast_shouldThrowException_ifListIsEmpty() {
        var list = new ArrayList<Integer>();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> Utils.getLast(list));
    }

    @Test
    void getCacheFile_shouldReturnLocationOfCacheFile() {
        assertThat(Utils.getCacheFile(session))
                .asString()
                .isEqualTo("/a/b/c/group/id/artifact/version/artifact-version-profile1-profile2.files");
    }
}

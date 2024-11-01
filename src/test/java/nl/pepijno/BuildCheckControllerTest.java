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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuildCheckControllerTest {

    private static final String RESOURCE_FILE_NAME = "myTestFile.files";

    private FileSystem fileSystem;
    private MavenSession session;
    private MavenProject project;

    @Mock
    private LifecyclePhasesHelper lifecyclePhasesHelper;

    @InjectMocks
    private BuildCheckController buildCheckController;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.osX());

        var localRepository = mock(ArtifactRepository.class);
        when(localRepository.getBasedir()).thenReturn(fileSystem.getPath("").toString());
        project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("group.id");
        when(project.getArtifactId()).thenReturn("artifact");
        when(project.getVersion()).thenReturn("version");

        session = mock(MavenSession.class);
        when(session.getLocalRepository()).thenReturn(localRepository);
    }

    //    @Test
    //    void removeCacheFile_shouldRemoveCacheFile_whenFileExists() throws IOException {
    //        final var resourceFilePath = fileSystem.getPath(RESOURCE_FILE_NAME);
    //        Files.copy(getResourceFilePath(), resourceFilePath);
    //        try (MockedStatic<Utils> mockUtils = mockStatic(Utils.class)) {
    //            mockUtils.when(() -> Utils.getCacheFile(session, project)).thenReturn(resourceFilePath);
    //            buildCheckController.removeCacheFile(session, project);
    //            assertThat(Files.exists(resourceFilePath)).isFalse();
    //        }
    //    }

    private Path getResourceFilePath() {
        final var resourceFilePath = getClass().getResource(RESOURCE_FILE_NAME).getPath();
        return Paths.get(resourceFilePath);
    }
}

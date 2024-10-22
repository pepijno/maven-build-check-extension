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

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.MojoExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifecyclePhasesHelperTest {

    private LifecyclePhasesHelper lifecyclePhasesHelper;
    private DefaultLifecycles defaultLifecycles;
    private Lifecycle cleanLifecycle;

    @BeforeEach
    void setUp() {
        defaultLifecycles = LifecyclesTestUtils.createDefaultLifecycles();
        cleanLifecycle = defaultLifecycles.getLifeCycles().stream()
                .filter(lifecycle -> lifecycle.getId().equals("clean"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Clean phase not found"));
        lifecyclePhasesHelper = new LifecyclePhasesHelper(defaultLifecycles, cleanLifecycle);
    }

    @Test
    void resolveHighestLifecyclePhase_shouldReturnHighestPhase() {
        final var phase = lifecyclePhasesHelper.resolveHighestLifecyclePhase(
                List.of(mockedMojoExecution("clean"), mockedMojoExecution("compile"), mockedMojoExecution("install")));
        assertThat(phase).isEqualTo("install");
    }

    @Test
    void isLaterPhaseThanClean_isTrue_forAllPhasesExceptClean() {
        final var afterClean = new ArrayList<>(defaultLifecycles.getLifeCycles());
        afterClean.remove(cleanLifecycle);

        assertThat(afterClean).isNotEmpty();
        assertThat(afterClean.stream().flatMap(it -> it.getPhases().stream()))
                .allMatch(lifecyclePhasesHelper::isLaterPhaseThanClean);
    }

    @Test
    void isLaterPhaseThanClean_isFalse_forCleanPhase() {
        assertThat(cleanLifecycle.getPhases()).noneMatch(lifecyclePhasesHelper::isLaterPhaseThanClean);
    }

    @Test
    void getCleanSegment_returnsListWithCleanSegment_ifPresent() {
        final var clean = mockedMojoExecution("clean");
        final var cleanSegment = lifecyclePhasesHelper.getCleanSegment(
                List.of(clean, mockedMojoExecution("compile"), mockedMojoExecution("install")));
        assertThat(cleanSegment).containsExactly(clean);
    }

    @Test
    void getCleanSegment_returnsEmptyList_ifNotPresent() {
        final var cleanSegment = lifecyclePhasesHelper.getCleanSegment(
                List.of(mockedMojoExecution("compile"), mockedMojoExecution("install")));
        assertThat(cleanSegment).isEmpty();
    }

    private static MojoExecution mockedMojoExecution(final String phase) {
        final var mojoExecution = mock(MojoExecution.class);
        when(mojoExecution.getLifecyclePhase()).thenReturn(phase);
        when(mojoExecution.toString()).thenReturn(phase);
        return mojoExecution;
    }
}

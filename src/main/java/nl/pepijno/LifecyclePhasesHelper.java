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

import java.util.List;

import org.apache.maven.SessionScoped;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.MojoExecution;

@SessionScoped
@Named
public class LifecyclePhasesHelper {

    private final List<String> phases;
    private final String lastCleanPhase;

    @Inject
    LifecyclePhasesHelper(final DefaultLifecycles defaultLifecycles, @Named("clean") final Lifecycle cleanLifecycle) {
        phases = defaultLifecycles.getLifeCycles().stream()
                .flatMap(lf -> lf.getPhases().stream())
                .toList();
        lastCleanPhase = Utils.getLast(cleanLifecycle.getPhases());
    }

    String resolveHighestLifecyclePhase(final List<MojoExecution> mojoExecutions) {
        return Utils.getLast(mojoExecutions).getLifecyclePhase();
    }

    boolean isLaterPhaseThanClean(final String phase) {
        return isLaterPhase(phase, lastCleanPhase);
    }

    List<MojoExecution> getCleanSegment(final List<MojoExecution> mojoExecutions) {
        return mojoExecutions.stream()
                .filter(mojoExecution -> !isLaterPhaseThanClean(mojoExecution.getLifecyclePhase()))
                .toList();
    }

    private boolean isLaterPhase(final String phase, final String other) {
        if (!phases.contains(phase)) {
            throw new IllegalArgumentException("Unsupported phase: " + phase);
        }
        if (!phases.contains(other)) {
            throw new IllegalArgumentException("Unsupported phase: " + other);
        }

        return phases.indexOf(phase) > phases.indexOf(other);
    }
}

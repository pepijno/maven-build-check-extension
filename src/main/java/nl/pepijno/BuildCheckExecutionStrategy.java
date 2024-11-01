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

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

@SessionScoped
@Named
@Priority(10)
public class BuildCheckExecutionStrategy implements MojosExecutionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(BuildCheckExecutionStrategy.class);

    private final LifecyclePhasesHelper lifecyclePhasesHelper;
    private final BuildCheckController buildCheckController;
    private final Config config;

    @Inject
    public BuildCheckExecutionStrategy(
            final LifecyclePhasesHelper lifecyclePhasesHelper,
            final BuildCheckController buildCheckController,
            final Config config) {
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
        this.buildCheckController = buildCheckController;
        this.config = config;
    }

    @Override
    public void execute(
            final List<MojoExecution> mojoExecutions,
            final MavenSession session,
            final MojoExecutionRunner mojoExecutionRunner)
            throws LifecycleExecutionException {
        final var source = getSource(mojoExecutions);

        var shouldRebuild = true;
        if (source == MojoExecution.Source.LIFECYCLE) {
            var cleanPhase = lifecyclePhasesHelper.getCleanSegment(mojoExecutions);
            for (var mojoExecution : cleanPhase) {
                mojoExecutionRunner.run(mojoExecution);
                removeCacheFile(session);
            }
            if (!config.isBuildCheckEnabled()) {
                LOG.info("Build check is disabled");
            } else if (!cleanPhase.isEmpty()) {
                LOG.info("Clean present, build check is disabled");
            } else {
                shouldRebuild = buildCheckController.shouldRebuild(session, mojoExecutions);
            }
        }

        if (shouldRebuild) {
            for (var mojoExecution : mojoExecutions) {
                if (source == MojoExecution.Source.CLI
                        || mojoExecution.getLifecyclePhase() == null
                        || lifecyclePhasesHelper.isLaterPhaseThanClean(mojoExecution.getLifecyclePhase())) {
                    mojoExecutionRunner.run(mojoExecution);
                    if (config.isBuildCheckEnabled() && "install".equals(mojoExecution.getLifecyclePhase())) {
                        buildCheckController.save(session);
                    }
                }
            }
        }
    }

    private void removeCacheFile(final MavenSession session) {
        LOG.debug("Removing cache file for project {}", session.getCurrentProject());
        buildCheckController.removeCacheFile(session, session.getCurrentProject());
    }

    private MojoExecution.Source getSource(final List<MojoExecution> mojoExecutions) {
        if (mojoExecutions == null || mojoExecutions.isEmpty()) {
            return null;
        }
        return mojoExecutions.stream()
                .map(MojoExecution::getSource)
                .filter(source -> source.equals(MojoExecution.Source.CLI))
                .findFirst()
                .orElse(MojoExecution.Source.LIFECYCLE);
    }
}

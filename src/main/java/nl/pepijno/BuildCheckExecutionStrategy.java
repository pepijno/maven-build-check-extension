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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionRunner;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            LifecyclePhasesHelper lifecyclePhasesHelper, BuildCheckController buildCheckController, Config config) {
        this.lifecyclePhasesHelper = lifecyclePhasesHelper;
        this.buildCheckController = buildCheckController;
        this.config = config;
    }

    @Override
    public void execute(
            List<MojoExecution> mojoExecutions, MavenSession session, MojoExecutionRunner mojoExecutionRunner)
            throws LifecycleExecutionException {
        final var project = session.getCurrentProject();

        final var source = getSource(mojoExecutions);

        var shouldRebuild = true;
        if (source == MojoExecution.Source.LIFECYCLE) {
            var cleanPhase = lifecyclePhasesHelper.getCleanSegment(mojoExecutions);
            for (MojoExecution mojoExecution : cleanPhase) {
                mojoExecutionRunner.run(mojoExecution);
            }
            if (!config.isBuildCheckEnabled()) {
                LOG.info("Build check is disabled");
            } else if (!cleanPhase.isEmpty()) {
                LOG.info("Clean present, build check is disabled");
            } else {
                shouldRebuild = buildCheckController.shouldRebuild(session, project, mojoExecutions);
            }
        }

        if (shouldRebuild) {
            for (MojoExecution mojoExecution : mojoExecutions) {
                if (source == MojoExecution.Source.CLI
                        || mojoExecution.getLifecyclePhase() == null
                        || lifecyclePhasesHelper.isLaterPhaseThanClean(mojoExecution.getLifecyclePhase())) {
                    mojoExecutionRunner.run(mojoExecution);
                    if ("compile".equals(mojoExecution.getLifecyclePhase())) {
                        LOG.debug("Removing downstream files for project {}", project);
                        buildCheckController.removeCacheFiles(session, project);
                    }
                    if ("install".equals(mojoExecution.getLifecyclePhase())) {
                        buildCheckController.save(session, project);
                    }
                }
            }
        }
    }

    private MojoExecution.Source getSource(List<MojoExecution> mojoExecutions) {
        if (mojoExecutions == null || mojoExecutions.isEmpty()) {
            return null;
        }
        for (MojoExecution mojoExecution : mojoExecutions) {
            if (mojoExecution.getSource() == MojoExecution.Source.CLI) {
                return MojoExecution.Source.CLI;
            }
        }
        return MojoExecution.Source.LIFECYCLE;
    }
}

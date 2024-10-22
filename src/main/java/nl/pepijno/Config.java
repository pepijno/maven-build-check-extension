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

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;

@SessionScoped
@Named
public class Config {

    private static final String BUILD_CHECK_ENABLED = "build.check.enabled";

    private final MavenSession session;

    @Inject
    public Config(MavenSession session) {
        this.session = session;
    }

    boolean isBuildCheckEnabled() {
        return getProperty(BUILD_CHECK_ENABLED, false);
    }

    private boolean getProperty(String key, boolean defaultValue) {
        String value = session.getUserProperties().getProperty(key);
        if (value == null) {
            value = session.getSystemProperties().getProperty(key);
            if (value == null) {
                return defaultValue;
            }
        }
        return Boolean.parseBoolean(value);
    }
}

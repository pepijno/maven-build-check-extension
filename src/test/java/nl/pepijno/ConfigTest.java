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

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfigTest {

    @Mock
    private MavenSession session;

    @InjectMocks
    private Config config;

    private Properties userProperties;
    private Properties systemProperties;

    @BeforeEach
    void setUp() {
        userProperties = new Properties();
        systemProperties = new Properties();
        when(session.getUserProperties()).thenReturn(userProperties);
        when(session.getSystemProperties()).thenReturn(systemProperties);
    }

    @Test
    void isBuildCheckEnabled_shouldReturnTrue_ifCheckPresentInUserProperties() {
        userProperties.setProperty("build.check.enabled", "true");
        assertThat(config.isBuildCheckEnabled()).isTrue();
    }

    @Test
    void isBuildCheckEnabled_shouldReturnTrue_ifCheckPresentInSystemProperties() {
        systemProperties.setProperty("build.check.enabled", "true");
        assertThat(config.isBuildCheckEnabled()).isTrue();
    }

    @Test
    void isBuildCheckEnabled_shouldReturnFalse_ifCheckNotPresentInUserPropertiesAndSystemProperties() {
        assertThat(config.isBuildCheckEnabled()).isFalse();
    }
}

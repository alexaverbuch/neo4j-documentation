/*
 * Copyright (c) 2002-2018 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.doc.jmx;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.causalclustering.discovery.Cluster;
import org.neo4j.causalclustering.discovery.CoreClusterMember;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.util.FileUtil;
import org.neo4j.test.causalclustering.ClusterRule;

import javax.management.ObjectInstance;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class CausalClusterJmxDocsTest {

    private static final String QUERY = "org.neo4j:instance=kernel#0,*";
    private static final int EXPECTED_NUMBER_OF_BEANS = 13;
    private final Path outPath = Paths.get("target", "docs", "ops");

    @Rule
    public final ClusterRule clusterRule = new ClusterRule();

    private JmxBeanDocumenter jmxBeanDocumenter;
    private FileUtil fileUtil;
    private Cluster cluster;

    @Before
    public void init() {
        this.jmxBeanDocumenter = new JmxBeanDocumenter();
        this.fileUtil = new FileUtil(outPath, "jmx-%s.adoc");
    }

    @Test
    public void shouldFindCausalClusteringJmxBeans() throws Exception {
        // given
        cluster = clusterRule
                .withNumberOfCoreMembers(2)
                .withInstanceCoreParam(setting("jmx.port", STRING, NO_DEFAULT), id -> Integer.toString(9913 + id))
                .startCluster();
        CoreClusterMember coreClusterMember = cluster.getCoreMemberById(0);

        // when
        String core0JmxPort = coreClusterMember.settingValue("jmx.port");
        String core1JmxPort = cluster.getCoreMemberById(1).settingValue("jmx.port");

        // then
        assertEquals("9913", core0JmxPort);
        assertNotEquals("9913", core1JmxPort);

        // when
        List<ObjectInstance> objectInstances = jmxBeanDocumenter.query(QUERY).stream()
                .sorted(Comparator.comparing(o -> o.getObjectName().getKeyProperty("name").toLowerCase()))
                .collect(Collectors.toList());

        // then
        assertEquals("Sanity checking the number of beans found;", EXPECTED_NUMBER_OF_BEANS, objectInstances.size());

        jmxBeanDocumenter.document(
                QUERY,
                it -> true,
                fileUtil,
                new AsciiDocListGenerator("jmx-list", "MBeans exposed by Neo4j", false)
        );
    }

}

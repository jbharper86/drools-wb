/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.dtable.backend.server.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.drools.workbench.models.datamodel.imports.Import;
import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.type.GuidedDTableResourceTypeDefinition;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.indexing.RuleAttributeNameAnalyzer;
import org.kie.workbench.common.services.refactoring.model.index.IndexableElements;
import org.uberfire.java.nio.file.Path;
import org.uberfire.metadata.backend.lucene.index.LuceneIndex;
import org.uberfire.metadata.backend.lucene.util.KObjectUtil;
import org.uberfire.metadata.engine.Index;
import org.uberfire.metadata.model.KObject;

import static org.apache.lucene.util.Version.*;
import static org.junit.Assert.*;

public class IndexGuidedDecisionTableActionsTest extends BaseIndexingTest<GuidedDTableResourceTypeDefinition> {

    @Test
    @Ignore("GuidedDecisionTableFileIndexer's use of PackageDescrVisitor doesn't support RHS sections yet")
    public void testIndexGuidedDecisionTableActions() throws IOException, InterruptedException {
        //Don't ask, but we need to write a single file first in order for indexing to work
        final Path basePath = getDirectoryPath().resolveSibling( "someNewOtherPath" );
        ioService().write( basePath.resolve( "dummy" ),
                           "<none>" );

        //Add test files
        final Path path = basePath.resolve( "dtable1.gdst" );
        final GuidedDecisionTable52 model = GuidedDecisionTableFactory.makeTableWithActionCol( "org.drools.workbench.screens.guided.dtable.backend.server.indexing",
                                                                                               new ArrayList<Import>() {{
                                                                                                   add( new Import( "org.drools.workbench.screens.guided.dtable.backend.server.indexing.classes.Applicant" ) );
                                                                                               }},
                                                                                               "dtable1" );
        final String xml = GuidedDTXMLPersistence.getInstance().marshal( model );
        ioService().write( path,
                           xml );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        final Index index = getConfig().getIndexManager().get( org.uberfire.metadata.io.KObjectUtil.toKCluster( basePath.getFileSystem() ) );

        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );

            final BooleanQuery query = new BooleanQuery();
            query.add( new TermQuery( new Term( IndexableElements.TYPE_NAME.toString(),
                                                "org.drools.workbench.screens.guided.dtable.backend.server.indexing.classes.applicant" ) ),
                       BooleanClause.Occur.MUST );
            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 1,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

    }

    @Override
    protected TestIndexer getIndexer() {
        return new TestGuidedDecisionTableFileIndexer();
    }

    @Override
    public Map<String, Analyzer> getAnalyzers() {
        return new HashMap<String, Analyzer>() {{
            put( IndexableElements.RULE_ATTRIBUTE_NAME.toString(),
                 new RuleAttributeNameAnalyzer( LUCENE_40 ) );
        }};
    }

    @Override
    protected GuidedDTableResourceTypeDefinition getResourceTypeDefinition() {
        return new GuidedDTableResourceTypeDefinition();
    }

    @Override
    protected String getRepositoryName() {
        return this.getClass().getSimpleName();
    }

}

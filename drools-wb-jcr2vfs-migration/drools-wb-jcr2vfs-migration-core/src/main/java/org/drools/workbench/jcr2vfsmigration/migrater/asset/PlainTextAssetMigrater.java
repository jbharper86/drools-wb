package org.drools.workbench.jcr2vfsmigration.migrater.asset;

import java.util.Map;
import java.util.Scanner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.rpc.Module;
import org.drools.guvnor.server.RepositoryAssetService;
import org.drools.repository.AssetItem;
import org.drools.workbench.jcr2vfsmigration.migrater.util.MigrationPathManager;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.base.options.CommentedOption;
import org.kie.commons.java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;

@ApplicationScoped
public class PlainTextAssetMigrater extends BaseAssetMigrater {

    protected static final Logger logger = LoggerFactory.getLogger( PlainTextAssetMigrater.class );

    @Inject
    protected RepositoryAssetService jcrRepositoryAssetService;

    @Inject
    private Paths paths;

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    protected MigrationPathManager migrationPathManager;

    public void migrate( Module jcrModule,
                         AssetItem jcrAssetItem) {
        Path path = migrationPathManager.generatePathForAsset( jcrModule,
                                                               jcrAssetItem );
        final org.kie.commons.java.nio.file.Path nioPath = paths.convert( path );
        if ( !Files.exists( nioPath ) ) {
            ioService.createFile( nioPath );
        }

        String content = jcrAssetItem.getContent();


        //Support for # has been removed from Drools Expert
        if (AssetFormats.DSL.equals(jcrAssetItem.getFormat())
                || AssetFormats.DSL_TEMPLATE_RULE.equals(jcrAssetItem.getFormat())                
        		|| AssetFormats.RULE_TEMPLATE.equals(jcrAssetItem.getFormat())
        		|| AssetFormats.DRL.equals(jcrAssetItem.getFormat())
                || AssetFormats.FUNCTION.equals(jcrAssetItem.getFormat())) {
        	StringBuffer sb = new StringBuffer();
            Scanner scanner = new Scanner(content);
            
            while (scanner.hasNextLine()) {
            	  String line = scanner.nextLine();
            	  if(line.startsWith("#")) {
            		  sb.append(line.replaceFirst("#", "//"));
            	  } else {
            		  sb.append(line);
            	  }            	  
            }
            
            scanner.close();
            content = sb.toString();
        }

        ioService.write( nioPath,
                         content,
                         migrateMetaData(jcrModule, jcrAssetItem),
                         new CommentedOption( jcrAssetItem.getLastContributor(),
                                              null,
                                              jcrAssetItem.getCheckinComment(),
                                              jcrAssetItem.getLastModified().getTime() ) );
    }

}

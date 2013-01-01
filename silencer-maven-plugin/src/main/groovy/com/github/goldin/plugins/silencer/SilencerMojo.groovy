package com.github.goldin.plugins.silencer

import static com.github.goldin.plugins.common.GMojoUtils.*
import com.github.goldin.plugins.common.BaseGroovyMojo
import org.apache.maven.LoggingRepositoryListener
import org.apache.maven.cli.AbstractMavenTransferListener
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder
import org.apache.maven.plugin.DefaultBuildPluginManager
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter


/**
 * Mojo for silencing Maven logging.
 */
@SuppressWarnings([ 'GroovyAccessibility' ])
@Mojo( name = 'silence', defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
class SilencerMojo extends BaseGroovyMojo
{
    @Parameter ( required = false )
    private String enabled

    @Parameter ( required = false )
    boolean logTime = false

    @Parameter ( required = false )
    boolean logMojo = false

    @Parameter ( required = false )
    boolean logSummary = false

    @Parameter ( required = false )
    String loggerFields

    @Component( role = LifecycleModuleBuilder )
    final LifecycleModuleBuilder moduleBuilder


    InterceptingMojoExecutor mojoExecutor


    final String defaultLoggerFields = '''
    org.apache.maven.plugin.compiler.CompilerMojo:compilerManager.compilers.javac.logger
    org.codehaus.gmaven.plugin.compile.CompileMojo:log
    org.codehaus.gmaven.plugin.compile.TestCompileMojo:log
    '''.stripIndent()


    @Override
    void doExecute()
    {
        if ( session.userProperties[ this.class.name ] != null ) { return }

        if (( enabled == null ) || ( enabled == 'true' ) || eval( enabled, Boolean ))
        {
            log.info( 'Silencer Mojo is on - enjoy the silence.' )

            tryIt { updateMavenExecutor() }
            tryIt { setFieldValue( repoSession.repositoryListener, LoggingRepositoryListener,     'logger', silentLogger )}
            tryIt { setFieldValue( repoSession.transferListener,   AbstractMavenTransferListener, 'out',    nullPrintStream())}
        }

        session.userProperties[ this.class.name ] = 'true'
    }


    void updateMavenExecutor ()
    {
        final pluginManager              = ( DefaultBuildPluginManager ) moduleBuilder.mojoExecutor.pluginManager
        pluginManager.mavenPluginManager = new InterceptingMavenPluginManager( this, pluginManager.mavenPluginManager )
        // noinspection GroovyNestedAssignment
        moduleBuilder.mojoExecutor       = this.mojoExecutor = new InterceptingMojoExecutor( moduleBuilder.mojoExecutor )
    }
}

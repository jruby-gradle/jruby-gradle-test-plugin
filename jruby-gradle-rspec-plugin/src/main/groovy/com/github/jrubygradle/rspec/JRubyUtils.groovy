package com.github.jrubygradle.rspec

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.InvalidUserDataException
import org.gradle.api.DefaultTask
import org.gradle.api.Project
//import org.gradle.api.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.artifacts.Configuration


/**
 * @author Christian Meier
 */
public class JRubyUtils {

    private final Project project;
    private final String name;
    private final Configuration config;
    private final File gemDir;
    private final File jrubyCompleteJar;
  
    public JRubyUtils(Project project, Configuration config, String name){
        this.project = project
        this.config = config
        if (config == null) throw new RuntimeException()
        this.gemDir = new File(project.buildDir, "gems-${name}")
        this.jrubyCompleteJar = JRubyExecUtils.jrubyJar(config)
    }

    public void setupGemsAndJars() {
        // TODO would be nice to just pass-in the jrubyCompleteJar File here
        GemUtils.extractGems(project, config, config, gemDir, GemUtils.OverwriteAction.SKIP)
        GemUtils.setupJars(config, gemDir, GemUtils.OverwriteAction.OVERWRITE)
    }

    public void exec(List<String> arguments) {
        project.javaexec {
            classpath jrubyCompleteJar.absolutePath
            // JRuby looks on the classpath inside the 'bin' directory
            // for executables
            classpath gemDir.absolutePath
        
            main 'org.jruby.Main'
        
            //TODO args '-I' + JRubyExec.jarDependenciesGemLibPath(gemDir)
            args '-rjars/setup'
            args arguments
            
            environment 'GEM_HOME' : gemDir.absolutePath
            environment 'GEM_PATH' : gemDir.absolutePath
            environment 'JARS_HOME' : new File(gemDir.absolutePath, 'jars')
            environment 'JARS_LOCK' : new File(gemDir.absolutePath, 'Jars.lock')
        }
    }
}

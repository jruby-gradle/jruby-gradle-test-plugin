package com.github.jrubygradle.rspec

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.InvalidUserDataException
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Christian Meier
 */
class RSpec extends DefaultTask {

    static final String DEFAULT_VERSION = '3.3.0'
  
    @Input
    String version = DEFAULT_VERSION

    @Input
    String pattern

    @Input
    String jrubyVersion = project.jruby.defaultVersion

    void version(String version) {
        this.version = version
    }

    void pattern(String files) {
        this.pattern = files
    }

    void jrubyVersion(String version) {
        this.jrubyVersion = version
    }

    @Input
    Configuration configuration = project.configurations.maybeCreate(name)
    void configuration(Object config) {
        if (config instanceof String ) {
            this.configuration = project.configurations.getByName(config)
        }
        else {
            this.configuration = config
        }
    }

    @TaskAction
    void run() {
        JRubyUtils jruby = new JRubyUtils(project, configuration, name)
        jruby.setEnv( 'CI_REPORTS' : "${project.buildDir}/${name}" )
        jruby.setupGemsAndJars()

        List<String> args = ['-S', 'rspec', '--require', 'ci/reporter/rspec', '--format', 'RSpec::Core::Formatters::ProgressFormatter', '--format', 'CI::Reporter::RSpecFormatter']
        if (pattern != null) {
            args += ['--pattern', pattern]
        }

        String tags = System.getProperty("${name}.tags")
        if (tags != null) {
            tags.split(/\s+/).each { args += ['--tag', it] }
        }

        String file = System.getProperty("${name}.file")
        if (file != null) {
            args += [file]
        }

        jruby.exec(args)
    }
}

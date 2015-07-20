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
    String jrubyVersion = project.jruby.defaultVersion

    void version(String version) {
        this.version = version
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

        jruby.setupGemsAndJars()

        jruby.exec('-S', 'rspec')
    }
}

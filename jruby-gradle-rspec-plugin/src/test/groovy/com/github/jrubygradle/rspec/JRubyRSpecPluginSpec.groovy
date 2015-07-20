package com.github.jrubygradle.rspec

import java.nio.file.Files

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.process.internal.ExecException
import org.gradle.testfixtures.ProjectBuilder

import org.apache.tools.ant.util.TeeOutputStream

import spock.lang.Specification

/**
 * @author Christian Meier
 *
 */
class JRubyRSpecPluginSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")

    def project
    def specDir

    static String captureStdout(Closure closure) {
        OutputStream output = new ByteArrayOutputStream()
        PrintStream out = System.out
        try {
          System.out = new PrintStream(new TeeOutputStream(System.out, output))
          closure.call()
        }
        finally {
          System.out = out
        }
        output.toString()
    }

    static Set<String> fileNames(FileCollection fc) {
        Set<String> names = []
        fc.asFileTree.visit { fvd ->
            names.add(fvd.relativePath.toString())
        }
        return names
    }

    static Project setupProject() {
        Project project = ProjectBuilder.builder().build()

        //project.gradle.startParameter.offline = true

        project.buildscript {
            repositories {
                flatDir dirs : TESTREPO_LOCATION.absolutePath
            }
        }
        project.buildDir = TESTROOT
        project.apply plugin: 'com.github.jruby-gradle.rspec'
        //project.jruby.defaultRepositories = false
        project.repositories {
            flatDir dirs : TESTREPO_LOCATION.absolutePath
        }

        return project
    }

    void setup() {

        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = setupProject()
        specDir = new File(project.projectDir, 'spec').getAbsoluteFile()
    }

    def 'Checking tasks exist'() {
        expect:
            project.tasks.getByName('rspec')
    }

    def "Checking configurations exist"() {
        given:
            def cfg = project.configurations

        expect:
            cfg.getByName('rspec')
    }

    def "Checking jruby-complete jar is configured"() {
        given:
            project.evaluate()
            def cfg = project.configurations.getByName('rspec')

        expect:
            cfg.files.find { it.name.startsWith('jruby-complete-') }
            cfg.files.find { it.name.startsWith('rspec-') }
    }

    def "Run rspec with defaults and not specs"() {
        given:
            project.evaluate()
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        expect:
            output.contains( 'No examples found.' )
    }

    def "Run rspec with none default jruby versions"() {
        given:
            Task task = project.tasks.getByName('rspec')
            task.jrubyVersion = '1.7.20'
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/jruby-version/spec').getAbsoluteFile().toPath())
            project.evaluate()
            String output = captureStdout {
                task.run()
            }
        expect:
            output.contains( '1 example, 0 failures' )
    }

    def "Throw exception on test failure"() {
        when:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/failing/spec').getAbsoluteFile().toPath())
            project.evaluate()
            project.tasks.getByName('rspec').run()
        then:
            thrown(ExecException)
    }

    def "Run rspec"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            project.evaluate()
            Task task = project.tasks.getByName('rspec')
            String output = captureStdout {
                task.run()
            }
        expect:
            output.contains( '4 examples, 0 failures' )
    }

    def "Run rspec tasks separated"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            project.dependencies {
               rspec 'rubygems:leafy-metrics:0.6.0'
               rspec 'org.slf4j:slf4j-simple:1.6.4'
            }
            Task task = project.tasks.create( 'mine', RSpec)
            project.evaluate()
            String outputMine = captureStdout {
                task.run()
            }

            specDir.delete()
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/more/spec').getAbsoluteFile().toPath())
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        expect:
            outputMine.contains( '4 examples, 0 failures' )
            output.contains( '2 examples, 0 failures' )
    }

    def "Run rspec task with custom configuration"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/more/spec').getAbsoluteFile().toPath())
            project.configurations.create('some')
            project.dependencies {
               some 'rubygems:leafy-metrics:0.6.0'
               some 'org.slf4j:slf4j-simple:1.6.4'
            }
            RSpec task = (RSpec) project.tasks.create( 'mine', RSpec)
            task.configure {
              configuration('some')
            }
            project.evaluate()
            String output = captureStdout {
                task.run()
            }
        expect:
            output.contains( '2 examples, 0 failures' )
    }

    def "Run custom rspec version"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/rspec-version/spec').getAbsoluteFile().toPath())
            Task task = project.tasks.getByName('rspec')
            task.configure {
                version = '3.2.0'
            }
            project.evaluate()
            String output = captureStdout {
                task.run()
            }
        expect:
            output.contains( '1 example, 0 failures' )
    }

    def "Run custom rspec version separate from other tasks"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/rspec-version/spec').getAbsoluteFile().toPath())
            Task task = project.tasks.create('other', RSpec)
            task.configure {
                version = '3.2.0'
            }
            project.evaluate()
            String outputOther = captureStdout {
                task.run()
            }
            specDir.delete()
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        expect:
            outputOther.contains( '1 example, 0 failures' )
            output.contains( '4 examples, 0 failures' )
    }

    def "Run rspec with custom pattern"() {
        given:
            File specsDir = new File(project.projectDir, 'myspec').getAbsoluteFile()
            Files.createSymbolicLink(specsDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            Task task = project.tasks.create('other', RSpec)
            task.configure {
                pattern 'myspec/*_spec.rb'
            }
            project.evaluate()
            String outputOther = captureStdout {
                task.run()
            }
            specsDir.delete()
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        expect:
            output.contains( '0 examples, 0 failures' )
            outputOther.contains( '4 examples, 0 failures' )
    }

    def "Run rspec with directory picker via system properties"() {
        given:
            Task task = project.tasks.create('other', RSpec)
            project.evaluate()
            System.setProperty('rspec.file', new File('src/test/resources/simple/spec').absolutePath)
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
            String outputOther = captureStdout {
                task.run()
            }
        expect:
            outputOther.contains( '0 examples, 0 failures' )
            output.contains( '4 examples, 0 failures' )
    }

    def "Run rspec task with file picker via system properties"() {
        given:
            Task task = project.tasks.create('other', RSpec)
            project.evaluate()
            System.properties.remove('rspec.file')
            System.setProperty('other.file', new File('src/test/resources/simple/spec/one_spec.rb').absolutePath)
            String outputOther = captureStdout {
                project.tasks.getByName('rspec').run()
            }
            String output = captureStdout {
                task.run()
            }
        expect:
            outputOther.contains( '0 examples, 0 failures' )
            output.contains( '4 examples, 0 failures' )
    }

    def "fails rspec with file picker if file is missing"() {
        when:
            project.evaluate()
            System.setProperty('rspec.file', 'path/does/not/exists/one_spec.rb')
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        then:
            thrown(ExecException)
    }
}

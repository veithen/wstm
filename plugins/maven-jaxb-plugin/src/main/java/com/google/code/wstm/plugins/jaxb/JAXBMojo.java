/*
 * Copyright 2010 Andreas Veithen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.wstm.plugins.jaxb;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.ErrorReceiver;
import com.sun.tools.xjc.ModelLoader;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;

/**
 * @goal xjc
 * @phase generate-sources
 * @requiresDependencyResolution compile
 */
public class JAXBMojo extends AbstractMojo {
    private static final String EPISODE_FILE = "META-INF/sun-jaxb.episode";
    
    /**
     * @parameter expression="src/main/bindings"
     * @required
     */
    private File bindingDirectory;
    
    /**
     * @parameter expression="${project.build.directory}/generated-sources/xjc"
     * @required
     */
    private File generateDirectory;
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        new File(generateDirectory, "META-INF").mkdirs();

        Options options = new Options();
        
        // We need to disable strict checking because it would call our custom entity
        // resolver with relative URIs. Since the base URI is not provided, we cannot
        // resolve the entity.
        options.strictCheck = false;
        
        // Required for SCD support (needed by episode files)
        options.compatibilityMode = Options.EXTENSION;
        
        MavenEntityResolver resolver = new MavenEntityResolver(project);
        
        options.entityResolver = resolver;
        options.targetDir = generateDirectory;
        
        for (Artifact artifact : (Set<Artifact>)project.getDependencyArtifacts()) {
            if ("xsd".equals(artifact.getType())) {
                options.addGrammar(resolver.getInputSource(artifact));
            }
        }
        
        // Scan the compile classpath for JAXB episode files
        try {
            // TODO: probably we need to exclude the current project.build.directory
            for (String classpathElement : (List<String>)project.getCompileClasspathElements()) {
                File f = new File(classpathElement);
                System.out.println("Scanning " + f);
                if (f.isDirectory()) {
                    File episodeFile = new File(f, EPISODE_FILE);
                    if (episodeFile.exists()) {
                        options.addBindFile(episodeFile);
                    }
                } else {
                    try {
                        options.scanEpisodeFile(f);
                    } catch (BadCommandLineException ex) {
                        throw new MojoExecutionException("Failed to load " + f, ex.getCause());
                    }
                }
            }
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Failed to get compile classpath", ex);
        }
        
        try {
            options.parseArguments(new String[] { "-episode", generateDirectory.getPath() + "/" + EPISODE_FILE });
        } catch (BadCommandLineException ex) {
            throw new MojoExecutionException("XJC failed to understand -episode option", ex);
        }
        
        ErrorReceiver er = new ErrorReporter();
        Model model = ModelLoader.load(options, new JCodeModel(), er);
        if (model == null) {
            throw new MojoFailureException("Failed to generate code");
        }
        
        Outline outline = model.generateCode(options, er);
        try {
            CodeWriter writer = options.createCodeWriter();
            model.codeModel.build(writer);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write generated code", ex);
        }
        
        project.addCompileSourceRoot(generateDirectory.getPath());
        
        Resource generatedResource = new Resource();
        generatedResource.setDirectory(generateDirectory.getPath());
        generatedResource.addExclude("**/*.java");
        project.addResource(generatedResource);
    }

}

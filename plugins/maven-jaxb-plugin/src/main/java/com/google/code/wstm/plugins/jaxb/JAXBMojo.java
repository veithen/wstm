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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.ConsoleErrorReporter;
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
        generateDirectory.mkdirs();

        Options options = new Options();
        options.targetDir = generateDirectory;
        
        for (Artifact artifact : (Set<Artifact>)project.getDependencyArtifacts()) {
            if ("xsd".equals(artifact.getType())) {
                options.addGrammar(artifact.getFile());
            }
        }
        
        ErrorReceiver er = new ConsoleErrorReporter();
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
    }

}

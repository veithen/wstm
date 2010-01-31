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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.code.wstm.xjc.EntityResolverEx;

public class MavenEntityResolver implements EntityResolver {
    private static final String URI_SCHEME = "maven";
    private static final String URI_PREFIX = URI_SCHEME + ":/";
    
    private final MavenProject project;
    
    public MavenEntityResolver(MavenProject project) {
        this.project = project;
    }

    public InputSource getInputSource(Artifact artifact) {
        InputSource is = new InputSource();
        is.setSystemId(URI_PREFIX + artifact.getGroupId().replace('.', '/') + "/" + artifact.getArtifactId() + "." + artifact.getType());
        try {
            is.setByteStream(new FileInputStream(artifact.getFile()));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Oops, got a non existing file from Maven: " + artifact.getFile());
        }
        return is;
    }
    
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        System.out.println("Resolving " + systemId);
        if (systemId.startsWith(URI_PREFIX)) {
            int lastSlash = systemId.lastIndexOf('/');
            String groupId = systemId.substring(URI_PREFIX.length(), lastSlash).replace('/', '.');
            int dot = systemId.lastIndexOf('.');
            String artifactId = systemId.substring(lastSlash+1, dot);
            String type = systemId.substring(dot+1);
            Artifact artifact = (Artifact)project.getArtifactMap().get(groupId + ":" + artifactId);
            // TODO: check for null
            // TODO: check artifact type
            return getInputSource(artifact);
        } else {
            return new InputSource(systemId);
        }
    }
}

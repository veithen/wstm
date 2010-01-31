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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.code.wstm.xjc.EntityResolverEx;

public class MavenEntityResolver implements EntityResolverEx {
    private static final String URI_SCHEME = "maven";
    private static final String URI_PREFIX = URI_SCHEME + ":";
    
    private final MavenProject project;
    
    public MavenEntityResolver(MavenProject project) {
        this.project = project;
    }

    public InputSource getInputSource(Artifact artifact) {
        InputSource is = new InputSource();
        is.setSystemId(URI_PREFIX + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType());
        try {
            is.setByteStream(new FileInputStream(artifact.getFile()));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Oops, got a non existing file from Maven: " + artifact.getFile());
        }
        return is;
    }
    
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        return new InputSource(systemId);
    }

    public InputSource resolveRelativeURL(String namespaceURI, String baseURI, String relativeURI) throws SAXException, IOException {
        if (isAbsoluteURI(relativeURI)) {
            return resolveEntity(namespaceURI, relativeURI);
        } else if (baseURI.startsWith(URI_PREFIX)) {
            int idx = baseURI.indexOf(':', URI_PREFIX.length());
            if (idx == -1) {
                throw new SAXException("Invalid URI: " + baseURI);
            }
            String groupId = baseURI.substring(URI_PREFIX.length(), idx);
            // TODO: need to take '..' into account
            idx = relativeURI.lastIndexOf('.');
            String artifactId = relativeURI.substring(0, idx);
            String type = relativeURI.substring(idx+1);
            Artifact artifact = (Artifact)project.getArtifactMap().get(groupId + ":" + artifactId);
            // TODO: check for null
            // TODO: check artifact type
            return getInputSource(artifact);
        } else {
            return new InputSource(new URL(new URL(baseURI), relativeURI).toExternalForm());
        }
    }

    private static boolean isAbsoluteURI(String uri) {
        int idx = uri.indexOf(':');
        if (idx == -1) {
            return false;
        } else {
            for (int i=0; i<idx; i++) {
                char c = uri.charAt(i);
                if (c == '/' || c == '?' || c == '#') {
                    return false;
                }
            }
            return true;
        }
    }
}

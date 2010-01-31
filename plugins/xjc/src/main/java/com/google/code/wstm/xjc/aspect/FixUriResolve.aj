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
package com.google.code.wstm.xjc.aspect;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.xml.xsom.impl.util.Uri;

public aspect FixUriResolve {
    String around(String baseUri, String uriReference) throws IOException:
            execution(String Uri.resolve(String, String)) && args(baseUri, uriReference) {
        if (Uri.isAbsolute(uriReference)) {
            return uriReference;
        }
        if (baseUri == null) {
            throw new IOException("Unable to resolve relative URI " + uriReference + " without a base URI");
        }
        if (!Uri.isAbsolute(baseUri)) {
            throw new IOException("Unable to resolve relative URI " + uriReference + " because base URI is not absolute: " + baseUri);
        }
        try {
            return new URI(baseUri).resolve(uriReference).toString();
        } catch (URISyntaxException ex) {
            throw new MalformedURLException(ex.getMessage());
        }
    }
}

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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.code.wstm.xjc.EntityResolverEx;
import com.sun.xml.xsom.impl.parser.NGCCRuntimeEx;

// TODO: probably this will no longer be necessary
public aspect EntityResolverExSupport {
    InputSource around(NGCCRuntimeEx rt, String namespaceURI, String relativeURI) throws SAXException:
            execution(InputSource NGCCRuntimeEx.resolveRelativeURL(String, String)) && this(rt) && args(namespaceURI, relativeURI) {
        EntityResolver er = rt.parser.getEntityResolver();
        if (er instanceof EntityResolverEx) {
            try {
                return ((EntityResolverEx)er).resolveRelativeURL(namespaceURI, rt.getLocator().getSystemId(), relativeURI);
            } catch (IOException ex) {
                throw new SAXParseException(ex.getMessage(), rt.getLocator(), ex);
            }
        } else {
            return proceed(rt, namespaceURI, relativeURI);
        }
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.transformer;

import java.io.IOException;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

import org.apache.servicemix.jbi.helper.MessageUtil;
import org.apache.servicemix.jbi.jaxp.BytesSource;
import org.apache.servicemix.jbi.jaxp.ResourceSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.marshaler.PojoMarshaler;

/**
 * A simple transformer which copies the properties and content from the source message to the destination message.
 *
 * @version $Revision: 564374 $
 */
public class CopyTransformer implements MessageTransformer {

    private static final CopyTransformer INSTANCE = new CopyTransformer();

    private SourceTransformer sourceTransformer = new SourceTransformer();

    private boolean copyProperties;

    private boolean copyAttachments;

    private boolean copySecuritySubject;

    private boolean copyContent;

    public CopyTransformer() {
        this(true, true, true, true);
    }
    
    public CopyTransformer(boolean copySecuritySubject, boolean copyContent, boolean copyProperties, boolean copyAttachments) {
        super();
        this.copySecuritySubject = copySecuritySubject;
        this.copyContent = copyContent;
        this.copyProperties = copyProperties;
        this.copyAttachments = copyAttachments;
    }

    /**
     * @return the copyAttachments
     */
    public boolean isCopyAttachments() {
        return copyAttachments;
    }

    /**
     * @param copyAttachments the copyAttachments to set
     */
    public void setCopyAttachments(boolean copyAttachments) {
        this.copyAttachments = copyAttachments;
    }

    /**
     * @return the copyProperties
     */
    public boolean isCopyProperties() {
        return copyProperties;
    }

    /**
     * @param copyProperties the copyProperties to set
     */
    public void setCopyProperties(boolean copyProperties) {
        this.copyProperties = copyProperties;
    }

    /**
     * @return the copySecuritySubject
     */
    public boolean isCopySecuritySubject() {
        return copySecuritySubject;
    }

    /**
     * @param copySecuritySubject the copySecuritySubject to set
     */
    public void setCopySecuritySubject(boolean copySecuritySubject) {
        this.copySecuritySubject = copySecuritySubject;
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static CopyTransformer getInstance() {
        return INSTANCE;
    }

    public boolean transform(MessageExchange exchange, NormalizedMessage from,
            NormalizedMessage to) throws MessagingException {
        if (copyProperties) {
            copyProperties(from, to);
        }

        if (copyContent) {
            copyContent(from, to);
        }

        if (copyAttachments) {
            copyAttachments(from, to);
        }

        if (copySecuritySubject) {
            copySecuritySubject(from, to);
        }

        return true;
    }

    private void copyContent(NormalizedMessage from, NormalizedMessage to) throws MessagingException {
        Source content = from.getContent();
        if ((content instanceof StreamSource || content instanceof SAXSource)
                && !(content instanceof StringSource)
                && !(content instanceof BytesSource)
                && !(content instanceof ResourceSource)) {
            // lets avoid stream open exceptions by using a temporary format
            try {
                content = sourceTransformer.toDOMSource(from);
            } catch (TransformerException e) {
                throw new MessagingException(e);
            } catch (ParserConfigurationException e) {
                throw new MessagingException(e);
            } catch (IOException e) {
                throw new MessagingException(e);
            } catch (SAXException e) {
                throw new MessagingException(e);
            }
        }
        to.setContent(content);
    }
    
    public NormalizedMessage transform(MessageExchange exchange, NormalizedMessage in)
        throws MessagingException {
        NormalizedMessage out = new MessageUtil.NormalizedMessageImpl();
        transform(exchange, in, out);
        return out;
    }

    /**
     * Copies all of the properties from one message to another
     * 
     * @param from the message containing the properties
     * @param to the destination messages where the properties are set
     */
    public static void copyProperties(NormalizedMessage from,
            NormalizedMessage to) {
        for (Iterator iter = from.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            // Do not copy transient properties
            if (!PojoMarshaler.BODY.equals(name)) {
                Object value = from.getProperty(name);
                to.setProperty(name, value);
            }
        }
    }

    /**
     * Copies the attachments from a message to another message
     * 
     * @param from the message with the attachments
     * @param to the message to which attachments are added
     * @throws MessagingException if an attachment could not be added 
     */
    public static void copyAttachments(NormalizedMessage from,
            NormalizedMessage to) throws MessagingException {
        for (Iterator iter = from.getAttachmentNames().iterator(); iter
                .hasNext();) {
            String name = (String) iter.next();
            DataHandler value = from.getAttachment(name);
            to.addAttachment(name, value);
        }
    }

    /**
     * Copies the subject from a message to another message
     * 
     * @param from the message with the subject
     * @param to the message to which the subject is added
     * @throws MessagingException if an attachment could not be added 
     */
    public static void copySecuritySubject(NormalizedMessage from,
            NormalizedMessage to) throws MessagingException {
        to.setSecuritySubject(from.getSecuritySubject());
    }

}

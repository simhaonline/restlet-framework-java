/**
 * Copyright 2005-2011 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.emf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLOptions;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EMOFResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLOptionsImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.ext.emf.internal.EmfHtmlWriter;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;

/**
 * Representation based on the EMF library. It can serialize and deserialize
 * automatically in either XML or XMI.
 * 
 * @see <a href="http://www.eclipse.org/modeling/emf/">EMF project</a>
 * @author Jerome Louvel
 * @param <T>
 *            The type to wrap.
 */
public class EmfRepresentation<T extends EObject> extends OutputRepresentation {

    /** The (parsed) object to format. */
    private T object;

    /** The representation to parse. */
    private Representation representation;

    /**
     * Constructor.
     * 
     * @param mediaType
     *            The target media type. Supported values are
     *            {@link MediaType#APPLICATION_XMI},
     *            {@link MediaType#APPLICATION_ECORE} and XML media types.
     * @param object
     *            The object to format.
     */
    public EmfRepresentation(MediaType mediaType, T object) {
        super(mediaType);
        this.object = object;
        this.representation = null;
    }

    /**
     * Constructor.
     * 
     * @param representation
     *            The representation to parse.
     */
    public EmfRepresentation(Representation representation) {
        super(representation.getMediaType());
        this.object = null;
        this.representation = representation;
    }

    /**
     * Creates and configure an EMF resource. Not to be confused with a Restlet
     * resource.
     * 
     * @param mediaType
     *            The associated media type (ECore, XMI or XML).
     * @return A new configured EMF resource.
     */
    protected XMLResource createEmfResource(MediaType mediaType) {
        XMLResource result = null;

        if (MediaType.APPLICATION_ECORE.isCompatible(getMediaType())) {
            result = new EMOFResourceImpl();
        } else if (MediaType.APPLICATION_XMI.isCompatible(getMediaType())) {
            result = new XMIResourceImpl();
        } else {
            result = new XMLResourceImpl();
        }

        if (getCharacterSet() != null) {
            result.setEncoding(getCharacterSet().getName());
        } else {
            result.setEncoding(CharacterSet.UTF_8.getName());
        }

        // Set XML load options
        result.getDefaultLoadOptions().put(
                XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        result.getDefaultLoadOptions().put(
                XMLResource.OPTION_USE_LEXICAL_HANDLER, Boolean.TRUE);
        result.getDefaultLoadOptions().put(
                XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);

        // Set XML save options
        result.getDefaultSaveOptions().put(
                XMLResource.OPTION_EXTENDED_META_DATA, Boolean.TRUE);
        result.getDefaultSaveOptions().put(XMLResource.OPTION_LINE_WIDTH, 80);
        result.getDefaultSaveOptions().put(
                XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.TRUE);
        result.getDefaultSaveOptions().put(XMLResource.OPTION_SCHEMA_LOCATION,
                Boolean.TRUE);

        // Set other XML options
        XMLOptions xmlOptions = new XMLOptionsImpl();
        xmlOptions.setProcessAnyXML(true);
        xmlOptions.setProcessSchemaLocations(true);
        result.getDefaultLoadOptions().put(XMLResource.OPTION_XML_OPTIONS,
                xmlOptions);

        return result;
    }

    /**
     * Returns the loading options. Null by default.
     * 
     * @return The loading options.
     */
    protected Map<?, ?> getLoadOptions() {
        return null;
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        T result = null;

        if (this.object != null) {
            result = this.object;
        } else if (this.representation != null) {
            try {
                Resource emfResource = createEmfResource(this.representation
                        .getMediaType());
                emfResource.load(this.representation.getStream(),
                        getLoadOptions());
                result = (T) emfResource.getContents().get(0);
            } catch (IOException e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to parse the object with XStream.", e);
            }
        }

        return result;
    }

    /**
     * Returns the saving options. Null by default.
     * 
     * @return The saving options.
     */
    protected Map<?, ?> getSaveOptions() {
        return null;
    }

    /**
     * If this representation wraps an {@link EObject}, then it tries to write
     * it as either XML, XMI or ECore/EMOF depending on the media type set.
     * 
     * Note that in order to write this {@link EObject}, an EMF resource is
     * created, configured for proper serialization and the {@link EObject} is
     * then added to the content of this resource. This could has a side effect
     * of removing it from a previous resource/container.
     */
    @Override
    public void write(OutputStream outputStream) throws IOException {
        if (this.representation != null) {
            this.representation.write(outputStream);
        } else if (object != null) {
            if (MediaType.APPLICATION_ALL_XML.isCompatible(getMediaType())
                    || MediaType.TEXT_XML.isCompatible(getMediaType())
                    || MediaType.APPLICATION_XMI.isCompatible(getMediaType())
                    || MediaType.APPLICATION_ECORE.isCompatible(getMediaType())) {
                Resource emfResource = createEmfResource(getMediaType());
                emfResource.getContents().add((EObject) this.object);
                emfResource.save(outputStream, getSaveOptions());
            } else if (MediaType.TEXT_HTML.isCompatible(getMediaType())) {
                EmfHtmlWriter htmlWriter = new EmfHtmlWriter(getObject());
                htmlWriter.write(new OutputStreamWriter(outputStream,
                        ((getCharacterSet() == null) ? CharacterSet.ISO_8859_1
                                .getName() : getCharacterSet().getName())));
            }
        }
    }
}
/**
 * Copyright 2005-2009 Noelios Technologies.
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

package org.restlet.engine.http.connector;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Parameter;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.util.Series;

/**
 * Response wrapper for server HTTP calls.
 * 
 * @author Jerome Louvel
 */
public class ConnectedResponse extends Response {

    /**
     * Adds a new header to the given request.
     * 
     * @param response
     *            The response to update.
     * @param headerName
     *            The header name to add.
     * @param headerValue
     *            The header value to add.
     */
    public static void addHeader(Response response, String headerName,
            String headerValue) {
        if (response instanceof ConnectedResponse) {
            ((ConnectedResponse) response).getHeaders().add(headerName,
                    headerValue);
        }
    }

    /** The server IP address. */
    private final String serverAddress;

    /** The server IP port number. */
    private final int serverPort;

    /** Indicates if the server data was parsed and added. */
    private volatile boolean serverAdded;

    /**
     * Constructor.
     * 
     * @param context
     *            The context of the parent connector.
     * @param connection
     *            The parent network connection.
     * @param request
     *            The associated request.
     * @param version
     *            The protocol version.
     * @param serverAddress
     *            The server IP address.
     * @param serverPort
     *            The server IP port number.
     */
    public ConnectedResponse(Context context, ClientConnection connection,
            Request request, String version, int statusCode,
            String reasonPhrase, String serverAddress, int serverPort) {
        super(request);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        // Set the properties
        setStatus(Status.valueOf(statusCode), reasonPhrase);
    }

    /**
     * Returns the HTTP headers.
     * 
     * @return The HTTP headers.
     */
    @SuppressWarnings("unchecked")
    public Series<Parameter> getHeaders() {
        return (Series<Parameter>) getAttributes().get(
                HeaderConstants.ATTRIBUTE_HEADERS);
    }

    /**
     * Returns the server-specific information.
     * 
     * @return The server-specific information.
     */
    @Override
    public ServerInfo getServerInfo() {
        final ServerInfo result = super.getServerInfo();

        if (!this.serverAdded) {
            result.setAddress(this.serverAddress);
            result.setAgent(Engine.VERSION_HEADER);
            result.setPort(this.serverPort);
            this.serverAdded = true;
        }

        return result;
    }

}
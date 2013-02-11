/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.jetty.connector;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.Jetty;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.RequestWriter;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.Statuses;

import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import java.io.*;
import java.net.CookieStore;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link Connector} that utilizes the Jetty HTTP Client to send and receive
 * HTTP request and responses.
 *
 * @author Arul Dhesiaseelan (aruld@acm.org)
 */
public class JettyConnector extends RequestWriter implements Connector {

    private static final Logger LOGGER = Logger.getLogger(JettyConnector.class.getName());

    private final HttpClient client;
    private CookieStore cookieStore = null;

    /**
     * Create the new Jetty client connector.
     *
     * @param config client configuration.
     */
    public JettyConnector(Configuration config) {
        this.client = new HttpClient();
        if (config != null) {
            Boolean disableCookies = (Boolean) config.getProperties().get(JettyClientProperties.DISABLE_COOKIES);
            disableCookies = (disableCookies != null) ? disableCookies : false;

            AuthenticationStore auth = client.getAuthenticationStore();
            Object basicAuthProvider = config.getProperty(JettyClientProperties.BASIC_AUTH);
            if (basicAuthProvider != null && (basicAuthProvider instanceof BasicAuthentication)) {
                auth.addAuthentication((BasicAuthentication) basicAuthProvider);
            }

            final Object proxyUri = config.getProperties().get(JettyClientProperties.PROXY_URI);
            if (proxyUri != null) {
                final URI u = getProxyUri(proxyUri);
                ProxyConfiguration proxyConfig = new ProxyConfiguration(u.getHost(), u.getPort());
                client.setProxyConfiguration(proxyConfig);
            }

            if (disableCookies)
                client.setCookieStore(new HttpCookieStore.Empty());
        }

        try {
            client.start();
        } catch (Exception e) {
            throw new ClientException("Failed to start the client.", e);
        }
        this.cookieStore = client.getCookieStore();
    }

    private static URI getProxyUri(final Object proxy) {
        if (proxy instanceof URI) {
            return (URI) proxy;
        } else if (proxy instanceof String) {
            return URI.create((String) proxy);
        } else {
            throw new ClientException("The proxy URI (" + JettyClientProperties.PROXY_URI +
                    ") property MUST be an instance of String or URI");
        }
    }


    /**
     * Get the {@link HttpClient}.
     *
     * @return the {@link HttpClient}.
     */
    public HttpClient getHttpClient() {
        return client;
    }

    /**
     * Get the {@link CookieStore}.
     *
     * @return the {@link CookieStore} instance or null when
     *         ClientProperties.DISABLE_COOKIES set to true.
     */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    public ClientResponse apply(final ClientRequest clientRequest) throws ClientException {
        final Request request = translate(clientRequest);
        try {
            final ContentResponse response = request.send();
            final ClientResponse responseContext = new ClientResponse(Statuses.from(response.getStatus()), clientRequest);

            final HttpFields respHeaders = response.getHeaders();
            final Iterator<HttpField> itr = respHeaders.iterator();
            while (itr.hasNext()) {
                HttpField header = itr.next();
                List<String> list = responseContext.getHeaders().get(header.getName());
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(header.getValue());
                responseContext.getHeaders().addAll(header.getName(), list);
            }

            try {
                responseContext.setEntityStream(new HttpClientResponseInputStream(response));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, null, e);
            }

            if (!responseContext.hasEntity()) {
                responseContext.bufferEntity();
                responseContext.close();
            }

            return responseContext;


        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    private static final class HttpClientResponseInputStream extends FilterInputStream {

        HttpClientResponseInputStream(final ContentResponse response) throws IOException {
            super(getInputStream(response));
        }

        @Override
        public void close() throws IOException {
            super.close();
        }
    }

    private static InputStream getInputStream(final ContentResponse response) throws IOException {
        return new ByteArrayInputStream(response.getContent());
    }

    private Request translate(final ClientRequest clientRequest) {
        final String strMethod = clientRequest.getMethod();
        final URI uri = clientRequest.getUri();
        final Request request;

        switch (strMethod) {
            case "GET":
                request = client.newRequest(uri);
                request.method(HttpMethod.GET);
                break;
            case "POST":
                request = client.newRequest(uri);
                request.method(HttpMethod.POST);
                break;
            case "PUT":
                request = client.newRequest(uri);
                request.method(HttpMethod.PUT);
                break;
            case "DELETE":
                request = client.newRequest(uri);
                request.method(HttpMethod.DELETE);
                break;
            case "HEAD":
                request = client.newRequest(uri);
                request.method(HttpMethod.HEAD);
                break;
            case "OPTIONS":
                request = client.newRequest(uri);
                request.method(HttpMethod.OPTIONS);
                break;
            case "TRACE":
                request = client.newRequest(uri);
                request.method(HttpMethod.TRACE);
                break;
            case "CONNECT":
                request = client.newRequest(uri);
                request.method(HttpMethod.CONNECT);
                break;
            case "MOVE":
                request = client.newRequest(uri);
                request.method(HttpMethod.MOVE);
                break;
            default:
                throw new ClientException("Method " + strMethod + " not supported.");
        }

        request.followRedirects(PropertiesHelper.getValue(clientRequest.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS, true));

        final ContentProvider entity = getHttpEntity(clientRequest);
        if (entity != null) {
            request.content(entity);
        }
        writeOutBoundHeaders(clientRequest.getHeaders(), request);
        return request;
    }

    private static void writeOutBoundHeaders(final MultivaluedMap<String, Object> headers, final Request request) {
        for (Map.Entry<String, List<Object>> e : headers.entrySet()) {
            List<Object> vs = e.getValue();
            if (vs.size() == 1) {
                request.getHeaders().add(e.getKey(), vs.get(0).toString());
            } else {
                StringBuilder b = new StringBuilder();
                for (Object v : e.getValue()) {
                    if (b.length() > 0) {
                        b.append(',');
                    }
                    b.append(v);
                }
                request.getHeaders().add(e.getKey(), b.toString());
            }
        }
    }

    private ContentProvider getHttpEntity(final ClientRequest requestContext) {
        final Object entity = requestContext.getEntity();

        if (entity == null) {
            return null;
        }

        //TODO: Jetty content provider output stream support?
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            final RequestEntityWriter rew = this.getRequestEntityWriter(requestContext);
            rew.writeRequestEntity(outputStream);
        } catch (IOException e) {
            throw new ClientException("Failed to write request entity.", e);
        }
        return new BytesContentProvider(outputStream.toByteArray());
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final Request connectorRequest = translate(request);
        final Throwable[] failure = new Throwable[1];
        final org.eclipse.jetty.client.api.Response[] futureResponse = new org.eclipse.jetty.client.api.Response[1];
        try {
            buildAsyncRequest(connectorRequest).onResponseContent(new org.eclipse.jetty.client.api.Response.ContentListener() {
                @Override
                public void onContent(org.eclipse.jetty.client.api.Response connectorResponse, ByteBuffer content) {
                    final ClientResponse response = translate(request, connectorResponse, content);
                    callback.response(response);
                    futureResponse[0] = connectorResponse;
                }

            }).onResponseFailure(new org.eclipse.jetty.client.api.Response.FailureListener() {
                @Override
                public void onFailure(org.eclipse.jetty.client.api.Response response, Throwable ex) {
                    failure[0] = ex;
                }
            }).send(new org.eclipse.jetty.client.api.Response.CompleteListener() {
                @Override
                public void onComplete(Result connectorResponse) {
                    failure[0] = connectorResponse.getFailure();
                }

            });
            //TODO: Is there a better approach to return a response future?
            return Futures.immediateFuture(futureResponse);
        } catch (Throwable t) {
            failure[0] = t;
            callback.failure(t);
        }

        final SettableFuture<Object> errorFuture = SettableFuture.create();
        errorFuture.setException(failure[0]);
        return errorFuture;

    }

    private Request buildAsyncRequest(final Request connectorRequest) {
        final Request request = client.newRequest(connectorRequest.getURI()).method(connectorRequest.getMethod()).content(connectorRequest.getContent()).followRedirects(connectorRequest.isFollowRedirects());
        Iterator<HttpField> itr = connectorRequest.getHeaders().iterator();
        while (itr.hasNext()) {
            HttpField header = itr.next();
            request.getHeaders().add(header.getName(), header.getValue());
        }
        return request;
    }

    private ClientResponse translate(ClientRequest requestContext, final org.eclipse.jetty.client.api.Response original, ByteBuffer content) {
        final ClientResponse responseContext = new ClientResponse(Statuses.from(original.getStatus()), requestContext);

        Iterator<HttpField> itr = original.getHeaders().iterator();
        while (itr.hasNext()) {
            HttpField header = itr.next();
            List<String> list = responseContext.getHeaders().get(header.getName());
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(header.getValue());
            responseContext.getHeaders().addAll(header.getName(), list);
        }

        responseContext.setEntityStream(new ByteBufferBackedInputStream(content));

        return responseContext;

    }

    private static final class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public synchronized int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public synchronized int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

    @Override
    public String getName() {
        return "Jetty/" + Jetty.VERSION;
    }

    @Override
    public void close() {
        try {
            client.stop();
        } catch (Exception e) {
            throw new ClientException("Failed to stop the client.", e);
        }
    }
}

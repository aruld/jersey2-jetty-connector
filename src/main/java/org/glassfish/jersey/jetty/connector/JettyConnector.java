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
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.client.*;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.internal.Statuses;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedMap;
import java.io.*;
import java.net.CookieStore;
import java.net.URI;
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
 * <p/>
 * The following properties are only supported at construction of this class:
 * <ul>
 * <li>{@link ClientProperties#SSL_CONFIG}</li>
 * <li>{@link ClientProperties#ASYNC_THREADPOOL_SIZE}</li>
 * <li>{@link JettyClientProperties#BASIC_AUTH}</li>
 * <li>{@link JettyClientProperties#DISABLE_COOKIES}</li>
 * <li>{@link JettyClientProperties#PROXY_URI}</li>
 * <li>{@link JettyClientProperties#PROXY_USERNAME}</li>
 * <li>{@link JettyClientProperties#PROXY_PASSWORD}</li>
 * </ul>
 * <p/>
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
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
        SslConfig sslConfig = null;
        if (config != null) {
            sslConfig = PropertiesHelper.getValue(config.getProperties(), ClientProperties.SSL_CONFIG, SslConfig.class);
        }
        if (sslConfig != null) {
            final SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setSslContext(sslConfig.getSSLContext());
            this.client = new HttpClient(sslContextFactory);
        } else {
            this.client = new HttpClient();
        }

        if (config != null) {
            final Object threadPoolSize = config.getProperties().get(ClientProperties.ASYNC_THREADPOOL_SIZE);
            if (threadPoolSize != null && threadPoolSize instanceof Integer && (Integer) threadPoolSize > 0) {
                final String name = HttpClient.class.getSimpleName() + "@" + hashCode();
                final QueuedThreadPool threadPool = new QueuedThreadPool((Integer) threadPoolSize);
                threadPool.setName(name);
                client.setExecutor(threadPool);
            }
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
            throw new ProcessingException("Failed to start the client.", e);
        }
        this.cookieStore = client.getCookieStore();
    }

    private static URI getProxyUri(final Object proxy) {
        if (proxy instanceof URI) {
            return (URI) proxy;
        } else if (proxy instanceof String) {
            return URI.create((String) proxy);
        } else {
            throw new ProcessingException(LocalizationMessages.WRONG_PROXY_URI_TYPE(JettyClientProperties.PROXY_URI));
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
     *         JettyClientProperties.DISABLE_COOKIES set to true.
     */
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    @Override
    public ClientResponse apply(final ClientRequest clientRequest) throws ProcessingException {
        final Request request = translateRequest(clientRequest);
        final ContentProvider entity = getBytesProvider(clientRequest);
        if (entity != null) {
            request.content(entity);
        }

        try {
            final ContentResponse response = request.send();
            final ClientResponse responseContext = new ClientResponse(Statuses.from(response.getStatus()), clientRequest);

            final HttpFields respHeaders = response.getHeaders();
            final Iterator<HttpField> itr = respHeaders.iterator();
            while (itr.hasNext()) {
                HttpField header = itr.next();
                List<String> list = responseContext.getHeaders().get(header.getName());
                if (list == null) {
                    list = new ArrayList<String>();
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
            throw new ProcessingException(e);
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

    private Request translateRequest(final ClientRequest clientRequest) {
        final HttpMethod method = HttpMethod.fromString(clientRequest.getMethod());
        if (method == null) {
            throw new ProcessingException(LocalizationMessages.METHOD_NOT_SUPPORTED(clientRequest.getMethod()));
        }
        final URI uri = clientRequest.getUri();
        Request request = null;

        switch (method) {
            case GET:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case POST:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case PUT:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case DELETE:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case HEAD:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case OPTIONS:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case TRACE:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case CONNECT:
                request = client.newRequest(uri);
                request.method(method);
                break;
            case MOVE:
                request = client.newRequest(uri);
                request.method(method);
                break;
        }

        request.followRedirects(PropertiesHelper.getValue(clientRequest.getConfiguration().getProperties(), ClientProperties.FOLLOW_REDIRECTS, true));
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

    private ContentProvider getBytesProvider(final ClientRequest requestContext) {
        final Object entity = requestContext.getEntity();

        if (entity == null) {
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            final RequestEntityWriter rew = this.getRequestEntityWriter(requestContext);
            rew.writeRequestEntity(outputStream);
        } catch (IOException e) {
            throw new ProcessingException("Failed to write request entity.", e);
        }
        return new BytesContentProvider(outputStream.toByteArray());
    }

    private ContentProvider getStreamProvider(final ClientRequest requestContext) {
        final Object entity = requestContext.getEntity();

        if (entity == null) {
            return null;
        }

        OutputStreamContentProvider streamContentProvider = new OutputStreamContentProvider();
        try {
            final RequestEntityWriter rew = this.getRequestEntityWriter(requestContext);
            rew.writeRequestEntity(streamContentProvider.getOutputStream());
        } catch (IOException e) {
            throw new ProcessingException("Failed to write request entity.", e);
        }
        return streamContentProvider;
    }

    @Override
    public Future<?> apply(final ClientRequest request, final AsyncConnectorCallback callback) {
        final Request connectorRequest = translateRequest(request);
        final ContentProvider entity = getStreamProvider(request);
        if (entity != null) {
            connectorRequest.content(entity);
        }

        final Throwable[] failure = new Throwable[1];
        final ClientResponse[] response = new ClientResponse[1];
        final InputStreamResponseListener responseListener = new InputStreamResponseListener();
        try {
            buildAsyncRequest(connectorRequest)
                    .onResponseHeaders(new org.eclipse.jetty.client.api.Response.HeadersListener() {
                        @Override
                        public void onHeaders(Response connectorResponse) {
                            response[0] = translateResponse(request, connectorResponse, responseListener);
                            callback.response(response[0]);
                        }
                    })
                    .onResponseFailure(new org.eclipse.jetty.client.api.Response.FailureListener() {
                        @Override
                        public void onFailure(org.eclipse.jetty.client.api.Response response, Throwable ex) {
                            failure[0] = ex;
                        }
                    }).send(responseListener);
            return Futures.immediateFuture(response[0]);
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

    private ClientResponse translateResponse(ClientRequest requestContext, final org.eclipse.jetty.client.api.Response original, final InputStreamResponseListener contentListener) {
        final ClientResponse responseContext = new ClientResponse(Statuses.from(original.getStatus()), requestContext);

        Iterator<HttpField> itr = original.getHeaders().iterator();
        while (itr.hasNext()) {
            HttpField header = itr.next();
            List<String> list = responseContext.getHeaders().get(header.getName());
            if (list == null) {
                list = new ArrayList<String>();
            }
            list.add(header.getValue());
            responseContext.getHeaders().addAll(header.getName(), list);
        }

        responseContext.setEntityStream(contentListener.getInputStream());

        return responseContext;

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
            throw new ProcessingException("Failed to stop the client.", e);
        }
    }
}

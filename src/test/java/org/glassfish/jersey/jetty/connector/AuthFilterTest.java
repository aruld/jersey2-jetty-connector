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

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class AuthFilterTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(AuthFilterTest.class.getName());

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(AuthTest.AuthResource.class);
        config.register(new LoggingFilter(LOGGER, true));
        return config;
    }


    @Override
    protected void configureClient(ClientConfig clientConfig) {
        clientConfig.connector(new JettyConnector(clientConfig));
    }

    @Test
    public void testAuthGetWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().get();
        assertEquals("GET", response.readEntity(String.class));
    }

    @Test
    public void testAuthPostWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().post(Entity.text("POST"));
        assertEquals("POST", response.readEntity(String.class));
    }


    @Test
    public void testAuthDeleteWithClientFilter() {
        client().register(new HttpBasicAuthFilter("name", "password"));
        Response response = target("test/filter").request().delete();
        assertEquals(204, response.getStatus());
    }

}

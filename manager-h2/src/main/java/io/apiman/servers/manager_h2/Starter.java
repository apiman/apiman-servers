/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.servers.manager_h2;

import io.apiman.manager.api.micro.ManagerApiMicroService;
import io.apiman.manager.api.micro.Users;

import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Starts up the API Manager.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class Starter {

    public static void main(String [] args) throws Exception {
        createDataSource();

        URL propsRes = Starter.class.getClassLoader().getResource("manager_h2-apiman.properties"); //$NON-NLS-1$
        if (propsRes != null) {
            System.setProperty("apiman.micro.manager.properties-url", propsRes.toString());
        }
        URL usersRes = Starter.class.getClassLoader().getResource("users.list"); //$NON-NLS-1$
        if (usersRes != null) {
            System.setProperty(Users.USERS_FILE_PROP, usersRes.toString());
        }

        ManagerApiMicroService micro = new ManagerApiMicroService() {
            @Override
            public int serverPort() {
                return 8080;
            }
        };
        micro.start();
    }

    /**
     * Creates a datasource and binds it to JNDI.
     */
    private static void createDataSource() {
        HikariConfig config = new HikariConfig("src/main/resources/hikari.properties");
        HikariDataSource ds = new HikariDataSource(config);
        try {
            InitialContext ctx = new InitialContext();
            ensureCtx(ctx, "java:/comp/env");
            ensureCtx(ctx, "java:/comp/env/jdbc");
            ctx.bind("java:/comp/env/jdbc/ApiManagerDS", ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensure that the given name is bound to a context.
     * @param ctx
     * @param name
     * @throws NamingException
     */
    private static void ensureCtx(InitialContext ctx, String name) throws NamingException {
        try {
            ctx.bind(name, new InitialContext());
        } catch (NameAlreadyBoundException e) {
            // this is ok
        }
    }

}

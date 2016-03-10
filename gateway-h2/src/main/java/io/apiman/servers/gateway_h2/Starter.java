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
package io.apiman.servers.gateway_h2;

import io.apiman.common.util.ddl.DdlParser;
import io.apiman.gateway.platforms.war.micro.GatewayMicroService;
import io.apiman.gateway.platforms.war.micro.Users;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import org.apache.commons.io.IOUtils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Start up a micro gateway.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class Starter {
    /**
     * Main entry point for the API Gateway micro service.
     * @param args the arguments
     * @throws Exception when any unhandled exception occurs
     */
    public static final void main(String [] args) throws Exception {
        URL resource = Starter.class.getClassLoader().getResource("users.list"); //$NON-NLS-1$
        if (resource != null) {
            System.setProperty(Users.USERS_FILE_PROP, resource.toString());
        }
        
        createDataSource();
        
        GatewayMicroService microService = new GatewayMicroService();
        loadProperties();
        microService.start();
        microService.join();
    }

    /**
     * Loads properties from a file and puts them into system properties.
     */
    @SuppressWarnings({ "unchecked" })
    protected static void loadProperties() {
        URL configUrl = Starter.class.getClassLoader().getResource("gateway_h2-apiman.properties");
        if (configUrl == null) {
            throw new RuntimeException("Failed to find properties file (see README.md): gateway_h2-apiman.properties");
        }
        InputStream is = null;
        try {
            is = configUrl.openStream();
            Properties props = new Properties();
            props.load(is);
            Enumeration<String> names = (Enumeration<String>) props.propertyNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = props.getProperty(name);
                System.setProperty(name, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Creates a datasource and binds it to JNDI.
     */
    private static void createDataSource() {
        HikariConfig config = new HikariConfig("src/main/resources/hikari.properties");
        HikariDataSource ds = new HikariDataSource(config);
        
        Connection connection = null;
        try {
            connection = ds.getConnection();
            connection.setAutoCommit(true);
            
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM information_schema.tables WHERE table_name = 'APIS'");
            ResultSet rs = statement.executeQuery();
            if (!rs.next()) {
                initDB(connection);
            }
            
            connection.close();
        } catch (Exception e1) {
            if (connection != null) {
                try { connection.close(); } catch (Exception e) {}
            }
            ds.close();
            throw new RuntimeException(e1);
        }
        
        try {
            InitialContext ctx = new InitialContext();
            ensureCtx(ctx, "java:/comp/env");
            ensureCtx(ctx, "java:/comp/env/jdbc");
            ctx.bind("java:/comp/env/jdbc/ApiGatewayDS", ds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initialize the DB with the apiman gateway DDL.
     * @param connection
     */
    private static void initDB(Connection connection) throws Exception {
        System.out.println("Detected that the DDL has not yet been installed.  Installing the apiman gateway H2 DDL now.");
        ClassLoader cl = Starter.class.getClassLoader();
        URL resource = cl.getResource("ddls/apiman-gateway_h2.ddl");
        int numStatements = 0;
        try (InputStream is = resource.openStream()) {
            DdlParser ddlParser = new DdlParser();
            List<String> statements = ddlParser.parse(is);
            for (String sql : statements) {
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.execute();
                numStatements++;
            }
        }
        System.out.println("DDL successfully installed.  Total SQL statements run: " + numStatements);
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

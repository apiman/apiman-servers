package io.apiman.servers.manager_es;

import io.apiman.manager.api.micro.ManagerApiMicroService;
import io.apiman.manager.api.micro.Users;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.net.URL;

public class StarterBundle implements BundleActivator {

    ManagerApiMicroService micro;

    @Override public void start(BundleContext context) throws Exception {

        URL url_es_apiman = context.getBundle().getResource("manager_es-apiman.properties");
        URL url_users = context.getBundle().getResource("users.list");

        System.setProperty("apiman.micro.manager.properties-url", url_es_apiman.toURI().toString());
        System.setProperty(Users.USERS_FILE_PROP, url_users.toURI().toString());

        micro = new ManagerApiMicroService() {
            @Override
            public int serverPort() {
                return 8080;
            }
        };
        micro.start();
    }

    @Override public void stop(BundleContext bundleContext) throws Exception {
        micro.stop();
    }
}

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
package io.apiman.servers.manager_es;

import io.apiman.manager.api.micro.ManagerApiMicroService;
import io.apiman.manager.api.micro.Users;

import java.net.URL;

/**
 * Starts up the API Manager.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class Starter {

    public static void main(String [] args) throws Exception {
        URL propsRes = Starter.class.getClassLoader().getResource("manager_es-apiman.properties"); //$NON-NLS-1$
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

}

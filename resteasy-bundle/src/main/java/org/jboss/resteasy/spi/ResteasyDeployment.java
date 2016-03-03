package org.jboss.resteasy.spi;

import org.jboss.resteasy.core.*;
import org.jboss.resteasy.plugins.interceptors.SecurityInterceptor;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.resourcefactory.JndiComponentResourceFactory;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.*;
import org.jboss.resteasy.util.GetRestful;
import org.jboss.resteasy.util.PickConstructor;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResteasyDeployment {
    protected boolean deploymentSensitiveFactoryEnabled = false;
    protected boolean asyncJobServiceEnabled = false;
    protected int asyncJobServiceMaxJobResults = 100;
    protected long asyncJobServiceMaxWait = 300000;
    protected int asyncJobServiceThreadPoolSize = 100;
    protected String asyncJobServiceBasePath = "/asynch/jobs";
    protected String applicationClass;
    protected String injectorFactoryClass;
    protected Application application;
    protected boolean registerBuiltin = true;
    protected List<String> scannedResourceClasses = new ArrayList<String>();
    protected List<String> scannedProviderClasses = new ArrayList<String>();
    protected List<String> scannedJndiComponentResources = new ArrayList<String>();
    protected List<String> jndiComponentResources = new ArrayList<String>();
    protected List<String> providerClasses = new ArrayList<String>();
    protected List<Class> actualProviderClasses = new ArrayList<Class>();
    protected List<Object> providers = new ArrayList<Object>();
    protected boolean securityEnabled = false;
    protected List<String> jndiResources = new ArrayList<String>();
    protected List<String> resourceClasses = new ArrayList<String>();
    protected List<String> unwrappedExceptions = new ArrayList<String>();
    protected List<Class> actualResourceClasses = new ArrayList<Class>();
    protected List<ResourceFactory> resourceFactories = new ArrayList<ResourceFactory>();
    protected List<Object> resources = new ArrayList<Object>();
    protected Map<String, String> mediaTypeMappings = new HashMap<String, String>();
    protected Map<String, String> languageExtensions = new HashMap<String, String>();
    protected List<String> interceptorPrecedences = new ArrayList<String>();
    protected Map<String, String> interceptorBeforePrecedences = new HashMap<String, String>();
    protected Map<String, String> interceptorAfterPrecedences = new HashMap<String, String>();
    protected Map<Class, Object> defaultContextObjects = new HashMap<Class, Object>();
    protected Map<String, String> constructedDefaultContextObjects = new HashMap<String, String>();
    protected Registry registry;
    protected Dispatcher dispatcher;
    protected ResteasyProviderFactory providerFactory;
    protected ThreadLocalResteasyProviderFactory threadLocalProviderFactory;
    protected String paramMapping;

    public void start() {
        // it is very important that each deployment create their own provider factory
        // this allows each WAR to have their own set of providers
        if (providerFactory == null)
            providerFactory = new ResteasyProviderFactory();
        providerFactory.setRegisterBuiltins(registerBuiltin);

        if (deploymentSensitiveFactoryEnabled) {
            // the ThreadLocalResteasyProviderFactory pushes and pops this deployments providerFactory
            // on a ThreadLocal stack.  This allows each application/WAR to have their own providerFactory
            // and still be able to call ResteasyProviderFactory.getInstance()
            if (!(providerFactory instanceof ThreadLocalResteasyProviderFactory)) {
                if (ResteasyProviderFactory.peekInstance() == null || !(ResteasyProviderFactory
                        .peekInstance() instanceof ThreadLocalResteasyProviderFactory)) {

                    threadLocalProviderFactory = new ThreadLocalResteasyProviderFactory(providerFactory);
                    ResteasyProviderFactory.setInstance(threadLocalProviderFactory);
                }
            }
        } else {
            ResteasyProviderFactory.setInstance(providerFactory);
        }

        if (asyncJobServiceEnabled) {
            AsynchronousDispatcher asyncDispatcher = new AsynchronousDispatcher(providerFactory);
            asyncDispatcher.setMaxCacheSize(asyncJobServiceMaxJobResults);
            asyncDispatcher.setMaxWaitMilliSeconds(asyncJobServiceMaxWait);
            asyncDispatcher.setThreadPoolSize(asyncJobServiceThreadPoolSize);
            asyncDispatcher.setBasePath(asyncJobServiceBasePath);
            asyncDispatcher.getUnwrappedExceptions().addAll(unwrappedExceptions);
            dispatcher = asyncDispatcher;
            asyncDispatcher.start();
        } else {
            SynchronousDispatcher dis = new SynchronousDispatcher(providerFactory);
            dis.getUnwrappedExceptions().addAll(unwrappedExceptions);
            dispatcher = dis;
        }
        registry = dispatcher.getRegistry();

        dispatcher.getDefaultContextObjects().putAll(defaultContextObjects);
        dispatcher.getDefaultContextObjects().put(Providers.class, providerFactory);
        dispatcher.getDefaultContextObjects().put(Registry.class, registry);
        dispatcher.getDefaultContextObjects().put(Dispatcher.class, dispatcher);
        dispatcher.getDefaultContextObjects().put(InternalDispatcher.class, InternalDispatcher.getInstance());

        // push context data so we can inject it
        Map contextDataMap = ResteasyProviderFactory.getContextDataMap();
        contextDataMap.putAll(dispatcher.getDefaultContextObjects());

        try {
            if (injectorFactoryClass != null) {
                InjectorFactory injectorFactory = null;

                Thread thread = Thread.currentThread();
                ClassLoader loader = thread.getContextClassLoader();
                thread.setContextClassLoader(ResteasyDeployment.class.getClassLoader());
                try {
                    Class<?> clazz = Thread.currentThread().getContextClassLoader()
                            .loadClass(injectorFactoryClass);
                    Constructor constructor = clazz.getConstructor(ResteasyProviderFactory.class);
                    injectorFactory = (InjectorFactory) constructor.newInstance(providerFactory);

                    if (injectorFactory == null)
                        injectorFactory = (InjectorFactory) clazz.newInstance();

                } catch (ClassNotFoundException cnfe) {
                    throw new RuntimeException(Messages.MESSAGES.unableToFindInjectorFactory(), cnfe);
                } catch (Exception e) {
                    throw new RuntimeException(Messages.MESSAGES.unableToInstantiateInjectorFactory(), e);
                } finally {
                    thread.setContextClassLoader(loader);
                }

                providerFactory.setInjectorFactory(injectorFactory);
            }

            // feed context data map with constructed objects
            // see ResteasyContextParameters.RESTEASY_CONTEXT_OBJECTS
            if (constructedDefaultContextObjects != null && constructedDefaultContextObjects.size() > 0) {
                for (Map.Entry<String, String> entry : constructedDefaultContextObjects.entrySet()) {
                    Class<?> key = null;
                    try {
                        key = Thread.currentThread().getContextClassLoader().loadClass(entry.getKey());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(
                                Messages.MESSAGES.unableToInstantiateContextObject(entry.getKey()), e);
                    }
                    Object obj = createFromInjectorFactory(entry.getValue(), providerFactory);
                    LogMessages.LOGGER.creatingContextObject(entry.getKey(), entry.getValue());
                    defaultContextObjects.put(key, obj);
                    dispatcher.getDefaultContextObjects().put(key, obj);
                    contextDataMap.put(key, obj);

                }
            }

            // Interceptor preferences should come before provider registration or builtin.

            if (interceptorPrecedences != null) {
                for (String precedence : interceptorPrecedences) {
                    providerFactory.appendInterceptorPrecedence(precedence.trim());
                }
            }

            if (interceptorBeforePrecedences != null) {
                for (Map.Entry<String, String> ext : interceptorBeforePrecedences.entrySet()) {
                    providerFactory
                            .insertInterceptorPrecedenceBefore(ext.getKey().trim(), ext.getValue().trim());
                }
            }
            if (interceptorAfterPrecedences != null) {
                for (Map.Entry<String, String> ext : interceptorAfterPrecedences.entrySet()) {
                    providerFactory
                            .insertInterceptorPrecedenceAfter(ext.getKey().trim(), ext.getValue().trim());
                }
            }

            if (securityEnabled) {
                providerFactory.getServerPreProcessInterceptorRegistry().register(SecurityInterceptor.class);
            }

            if (registerBuiltin) {
                providerFactory.setRegisterBuiltins(true);
                RegisterBuiltin.register(providerFactory);
            } else {
                providerFactory.setRegisterBuiltins(false);
            }

            if (applicationClass != null) {
                application = createApplication(applicationClass, providerFactory);

            }

            // register all providers
            registration();

            if (paramMapping != null) {
                dispatcher.addHttpPreprocessor(new AcceptParameterHttpPreprocessor(paramMapping));
            }

            if (mediaTypeMappings != null) {
                Map<String, MediaType> extMap = new HashMap<String, MediaType>();
                for (Map.Entry<String, String> ext : mediaTypeMappings.entrySet()) {
                    String value = ext.getValue();
                    extMap.put(ext.getKey().trim(), MediaType.valueOf(value.trim()));
                }
                if (dispatcher.getMediaTypeMappings() != null)
                    dispatcher.getMediaTypeMappings().putAll(extMap);
                else
                    dispatcher.setMediaTypeMappings(extMap);
            }

            if (languageExtensions != null) {
                if (dispatcher.getLanguageMappings() != null)
                    dispatcher.getLanguageMappings().putAll(languageExtensions);
                else
                    dispatcher.setLanguageMappings(languageExtensions);
            }
        } finally {
            ResteasyProviderFactory.removeContextDataLevel();
        }
    }

    public static Application createApplication(String applicationClass,
            ResteasyProviderFactory providerFactory) {
        if (applicationClass == null || applicationClass.trim().equals("")) {
            throw new ApplicationException(Messages.MESSAGES.applicationMustBeSpecified(), null);
        }
        return (Application) createFromInjectorFactory(applicationClass, providerFactory);
    }

    public static Object createFromInjectorFactory(String classname,
            ResteasyProviderFactory providerFactory) {
        Class<?> clazz = null;
        try {
            Thread thread = Thread.currentThread();
            ClassLoader loader = thread.getContextClassLoader();
            thread.setContextClassLoader(ResteasyDeployment.class.getClassLoader());
            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(classname);
            } finally {
                thread.setContextClassLoader(loader);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Constructor<?> constructor = PickConstructor.pickSingletonConstructor(clazz);
        if (constructor == null) {
            throw new RuntimeException(
                    Messages.MESSAGES.unableToFindPublicConstructorForClass(clazz.getName()));
        }
        ConstructorInjector constructorInjector = providerFactory.getInjectorFactory()
                .createConstructor(constructor);
        PropertyInjector propertyInjector = providerFactory.getInjectorFactory()
                .createPropertyInjector(clazz);

        Object obj = constructorInjector.construct();
        propertyInjector.inject(obj);
        return obj;
    }

    public void registration() {
        boolean useScanning = true;
        if (application != null) {
            dispatcher.getDefaultContextObjects().put(Application.class, application);
            ResteasyProviderFactory.getContextDataMap().put(Application.class, application);
            if (processApplication(application)) {
                // Application class registered something so don't use scanning data.  See JAX-RS spec for more detail.
                useScanning = false;
            }
        }

        if (useScanning && scannedProviderClasses != null) {
            for (String provider : scannedProviderClasses) {
                registerProvider(provider);
            }
        }

        if (providerClasses != null) {
            for (String provider : providerClasses) {
                registerProvider(provider);
            }
        }
        if (providers != null) {
            for (Object provider : providers) {
                providerFactory.registerProviderInstance(provider);
            }
        }

        for (Class actualProviderClass : actualProviderClasses) {
            providerFactory.registerProvider(actualProviderClass);
        }

        // All providers should be registered before resources because of interceptors.
        // interceptors must exist as they are applied only once when the resource is registered.

        if (useScanning && scannedJndiComponentResources != null) {
            for (String resource : scannedJndiComponentResources) {
                registerJndiComponentResource(resource);
            }
        }
        if (jndiComponentResources != null) {
            for (String resource : jndiComponentResources) {
                registerJndiComponentResource(resource);
            }
        }
        if (jndiResources != null) {
            for (String resource : jndiResources) {
                registry.addJndiResource(resource.trim());
            }
        }

        if (useScanning && scannedResourceClasses != null) {
            for (String resource : scannedResourceClasses) {
                Class clazz = null;
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(resource.trim());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                registry.addPerRequestResource(clazz);
            }
        }
        if (resourceClasses != null) {
            for (String resource : resourceClasses) {
                Class clazz = null;
                try {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(resource.trim());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                registry.addPerRequestResource(clazz);
            }
        }

        if (resources != null) {
            for (Object obj : resources) {
                registry.addSingletonResource(obj);
            }
        }

        for (Class actualResourceClass : actualResourceClasses) {
            registry.addPerRequestResource(actualResourceClass);
        }

        for (ResourceFactory factory : resourceFactories) {
            registry.addResourceFactory(factory);
        }
    }

    protected void registerJndiComponentResource(String resource) {
        String[] config = resource.trim().split(";");
        if (config.length < 3) {
            throw new RuntimeException(Messages.MESSAGES.jndiComponentResourceNotSetCorrectly());
        }
        String jndiName = config[0];
        Class clazz = null;
        try {
            clazz = Thread.currentThread().getContextClassLoader().loadClass(config[1]);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(Messages.MESSAGES.couldNotFindClassJndi(config[1]), e);
        }
        boolean cacheRefrence = Boolean.valueOf(config[2].trim());
        JndiComponentResourceFactory factory = new JndiComponentResourceFactory(jndiName, clazz,
                cacheRefrence);
        getResourceFactories().add(factory);

    }

    public void stop() {
        if (asyncJobServiceEnabled) {
            ((AsynchronousDispatcher) dispatcher).stop();
        }

        ResteasyProviderFactory.clearInstanceIfEqual(threadLocalProviderFactory);
        ResteasyProviderFactory.clearInstanceIfEqual(providerFactory);
    }

    /**
     * @param config
     * @return whether application class registered anything. i.e. whether scanning metadata should be used or not
     */
    protected boolean processApplication(Application config) {
        LogMessages.LOGGER.deployingApplication(Application.class.getName(), config.getClass());
        boolean registered = false;
        if (config.getClasses() != null) {
            for (Class clazz : config.getClasses()) {
                if (GetRestful.isRootResource(clazz)) {
                    actualResourceClasses.add(clazz);
                    registered = true;
                } else if (clazz.isAnnotationPresent(Provider.class)) {
                    actualProviderClasses.add(clazz);
                    registered = true;
                } else {
                    // required by spec to warn and not abort
                    LogMessages.LOGGER.applicationGetClassesUnknownClassType(clazz.getName());
                }
            }
        }
        if (config.getSingletons() != null) {
            for (Object obj : config.getSingletons()) {
                if (GetRestful.isRootResource(obj.getClass())) {
                    LogMessages.LOGGER
                            .addingSingletonResource(obj.getClass().getName(), Application.class.getName());
                    resources.add(obj);
                    registered = true;
                } else if (obj.getClass().isAnnotationPresent(Provider.class)) {
                    providers.add(obj);
                    registered = true;
                } else {
                    // required by spec to warn and not abort
                    LogMessages.LOGGER.applicationGetSingletonsUnknownClassType(obj.getClass().getName());
                }
            }
        }
        return registered;
    }

    protected void registerProvider(String clazz) {
        Class provider = null;
        try {
            provider = Thread.currentThread().getContextClassLoader().loadClass(clazz.trim());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        providerFactory.registerProvider(provider);
    }

    public List<String> getJndiComponentResources() {
        return jndiComponentResources;
    }

    public void setJndiComponentResources(List<String> jndiComponentResources) {
        this.jndiComponentResources = jndiComponentResources;
    }

    public String getApplicationClass() {
        return applicationClass;
    }

    public void setApplicationClass(String applicationClass) {
        this.applicationClass = applicationClass;
    }

    public String getInjectorFactoryClass() {
        return injectorFactoryClass;
    }

    public void setInjectorFactoryClass(String injectorFactoryClass) {
        this.injectorFactoryClass = injectorFactoryClass;
    }

    public boolean isDeploymentSensitiveFactoryEnabled() {
        return deploymentSensitiveFactoryEnabled;
    }

    public void setDeploymentSensitiveFactoryEnabled(boolean deploymentSensitiveFactoryEnabled) {
        this.deploymentSensitiveFactoryEnabled = deploymentSensitiveFactoryEnabled;
    }

    public boolean isAsyncJobServiceEnabled() {
        return asyncJobServiceEnabled;
    }

    public void setAsyncJobServiceEnabled(boolean asyncJobServiceEnabled) {
        this.asyncJobServiceEnabled = asyncJobServiceEnabled;
    }

    public int getAsyncJobServiceMaxJobResults() {
        return asyncJobServiceMaxJobResults;
    }

    public void setAsyncJobServiceMaxJobResults(int asyncJobServiceMaxJobResults) {
        this.asyncJobServiceMaxJobResults = asyncJobServiceMaxJobResults;
    }

    public long getAsyncJobServiceMaxWait() {
        return asyncJobServiceMaxWait;
    }

    public void setAsyncJobServiceMaxWait(long asyncJobServiceMaxWait) {
        this.asyncJobServiceMaxWait = asyncJobServiceMaxWait;
    }

    public int getAsyncJobServiceThreadPoolSize() {
        return asyncJobServiceThreadPoolSize;
    }

    public void setAsyncJobServiceThreadPoolSize(int asyncJobServiceThreadPoolSize) {
        this.asyncJobServiceThreadPoolSize = asyncJobServiceThreadPoolSize;
    }

    public String getAsyncJobServiceBasePath() {
        return asyncJobServiceBasePath;
    }

    public void setAsyncJobServiceBasePath(String asyncJobServiceBasePath) {
        this.asyncJobServiceBasePath = asyncJobServiceBasePath;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public boolean isRegisterBuiltin() {
        return registerBuiltin;
    }

    public void setRegisterBuiltin(boolean registerBuiltin) {
        this.registerBuiltin = registerBuiltin;
    }

    public List<String> getProviderClasses() {
        return providerClasses;
    }

    public void setProviderClasses(List<String> providerClasses) {
        this.providerClasses = providerClasses;
    }

    public List<Object> getProviders() {
        return providers;
    }

    public void setProviders(List<Object> providers) {
        this.providers = providers;
    }

    public List<Class> getActualProviderClasses() {
        return actualProviderClasses;
    }

    public void setActualProviderClasses(List<Class> actualProviderClasses) {
        this.actualProviderClasses = actualProviderClasses;
    }

    public List<Class> getActualResourceClasses() {
        return actualResourceClasses;
    }

    public void setActualResourceClasses(List<Class> actualResourceClasses) {
        this.actualResourceClasses = actualResourceClasses;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public void setSecurityEnabled(boolean securityEnabled) {
        this.securityEnabled = securityEnabled;
    }

    public List<String> getJndiResources() {
        return jndiResources;
    }

    public void setJndiResources(List<String> jndiResources) {
        this.jndiResources = jndiResources;
    }

    public List<String> getResourceClasses() {
        return resourceClasses;
    }

    public void setResourceClasses(List<String> resourceClasses) {
        this.resourceClasses = resourceClasses;
    }

    public Map<String, String> getMediaTypeMappings() {
        return mediaTypeMappings;
    }

    public void setMediaTypeMappings(Map<String, String> mediaTypeMappings) {
        this.mediaTypeMappings = mediaTypeMappings;
    }

    public List<Object> getResources() {
        return resources;
    }

    public void setResources(List<Object> resources) {
        this.resources = resources;
    }

    public Map<String, String> getLanguageExtensions() {
        return languageExtensions;
    }

    public void setLanguageExtensions(Map<String, String> languageExtensions) {
        this.languageExtensions = languageExtensions;
    }

    public List<String> getInterceptorPrecedences() {
        return interceptorPrecedences;
    }

    public void setInterceptorPrecedences(List<String> interceptorPrecedences) {
        this.interceptorPrecedences = interceptorPrecedences;
    }

    public Map<String, String> getInterceptorBeforePrecedences() {
        return interceptorBeforePrecedences;
    }

    public void setInterceptorBeforePrecedences(Map<String, String> interceptorBeforePrecedences) {
        this.interceptorBeforePrecedences = interceptorBeforePrecedences;
    }

    public Map<String, String> getInterceptorAfterPrecedences() {
        return interceptorAfterPrecedences;
    }

    public void setInterceptorAfterPrecedences(Map<String, String> interceptorAfterPrecedences) {
        this.interceptorAfterPrecedences = interceptorAfterPrecedences;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ResteasyProviderFactory getProviderFactory() {
        return providerFactory;
    }

    public void setProviderFactory(ResteasyProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    public void setMediaTypeParamMapping(String paramMapping) {
        this.paramMapping = paramMapping;
    }

    public List<ResourceFactory> getResourceFactories() {
        return resourceFactories;
    }

    public void setResourceFactories(List<ResourceFactory> resourceFactories) {
        this.resourceFactories = resourceFactories;
    }

    public List<String> getUnwrappedExceptions() {
        return unwrappedExceptions;
    }

    public void setUnwrappedExceptions(List<String> unwrappedExceptions) {
        this.unwrappedExceptions = unwrappedExceptions;
    }

    public Map<String, String> getConstructedDefaultContextObjects() {
        return constructedDefaultContextObjects;
    }

    public void setConstructedDefaultContextObjects(Map<String, String> constructedDefaultContextObjects) {
        this.constructedDefaultContextObjects = constructedDefaultContextObjects;
    }

    public Map<Class, Object> getDefaultContextObjects() {
        return defaultContextObjects;
    }

    public void setDefaultContextObjects(Map<Class, Object> defaultContextObjects) {
        this.defaultContextObjects = defaultContextObjects;
    }

    public List<String> getScannedResourceClasses() {
        return scannedResourceClasses;
    }

    public void setScannedResourceClasses(List<String> scannedResourceClasses) {
        this.scannedResourceClasses = scannedResourceClasses;
    }

    public List<String> getScannedProviderClasses() {
        return scannedProviderClasses;
    }

    public void setScannedProviderClasses(List<String> scannedProviderClasses) {
        this.scannedProviderClasses = scannedProviderClasses;
    }

    public List<String> getScannedJndiComponentResources() {
        return scannedJndiComponentResources;
    }

    public void setScannedJndiComponentResources(List<String> scannedJndiComponentResources) {
        this.scannedJndiComponentResources = scannedJndiComponentResources;
    }
}
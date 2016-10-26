package io.sls.server.bootstrap;

import com.google.inject.Provides;
import io.sls.runtime.bootstrap.AbstractBaseModule;
import io.sls.server.IServerRuntime;
import io.sls.server.MongoLoginService;
import io.sls.server.ServerRuntime;
import io.sls.server.rest.resolvers.JacksonContextResolver;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.InputStream;

/**
 * @author ginccc
 */
public class ServerRuntimeModule extends AbstractBaseModule {
    public ServerRuntimeModule(InputStream... configFile) {
        super(configFile);
    }

    @Override
    protected void configure() {
        registerConfigFiles(configFiles);
        bind(LoginService.class).to(MongoLoginService.class);
        bind(JacksonContextResolver.class);
    }

    @Provides
    @Singleton
    public IServerRuntime provideServerRuntime(@Named("system.environment") String environment,
                                               @Named("webServer.host") String host,
                                               @Named("webServer.httpPort") Integer httpPort,
                                               @Named("webServer.httpsPort") Integer httpsPort,
                                               @Named("webServer.sslOnly") Boolean sslOnly,
                                               @Named("webServer.defaultPath") String defaultPath,
                                               @Named("webServer.relativePathKeystore") String relativePathKeystore,
                                               @Named("webServer.passwordKeystore") String passwordKeystore,
                                               @Named("webServer.responseDelayInMillis") Long responseDelayInMillis,
                                               @Named("webServer.basicAuthPaths") String basicAuthPaths,
                                               @Named("webServer.virtualHosts") String virtualHosts,
                                               @Named("webServer.useCrossSiteScriptingHeaderParam") Boolean useCrossSiteScriptingHeaderParam,
                                               @Named("webServer.baseUri") String baseUri,
                                               @Named("webServer.applicationConfigurationClass") String applicationConfigurationClass,
                                               GuiceResteasyBootstrapServletContextListener contextListener,
                                               HttpServletDispatcher httpServletDispatcher,
                                               SecurityHandler securityHandler,
                                               LoginService mongoLoginService) {

        try {
            ServerRuntime.Options options = new ServerRuntime.Options();
            options.applicationConfiguration = Class.forName(applicationConfigurationClass);
            options.loginService = mongoLoginService;
            options.host = host;
            options.httpPort = httpPort;
            options.httpsPort = httpsPort;
            options.sslOnly = sslOnly;
            options.defaultPath = defaultPath;
            options.pathKeystore = System.getProperty("user.dir") + relativePathKeystore;
            options.passwordKeystore = passwordKeystore;
            options.responseDelayInMillis = responseDelayInMillis;
            options.basicAuthPaths = basicAuthPaths.split(";");
            options.virtualHosts = virtualHosts.split(";");
            options.useCrossSiteScripting = useCrossSiteScriptingHeaderParam;

            return new ServerRuntime(options, contextListener, httpServletDispatcher, securityHandler, environment, baseUri);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Provides
    @Singleton
    private SecurityHandler provideAuthenticationService(@Named("webserver.keycloak.admin") String admin,
                                                         @Named("webserver.keycloak.user") String user,
                                                         @Named("webserver.keycloak.path") String path,
                                                         @Named("webserver.keycloak.realm") String realm,
                                                         @Named("webserver.keycloak.realmKey") String realmKey,
                                                         @Named("webserver.keycloak.authServerUrl") String authServerUrl,
                                                         @Named("webserver.keycloak.sslRequired") String sslRequired,
                                                         @Named("webserver.keycloak.resource") String resource,
                                                         @Named("webserver.keycloak.publicClient") Boolean publicClient) {
        // Standard Login
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addRole(admin);
        securityHandler.addRole(user);

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec(path);
        Constraint constraint = new Constraint();
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{admin, user});

        mapping.setConstraint(constraint);
        securityHandler.addConstraintMapping(mapping);

        // Keycloak
        KeycloakJettyAuthenticator keycloakAuthenticator = new KeycloakJettyAuthenticator();
        AdapterConfig keycloakAdapterConfig = new AdapterConfig();
        keycloakAdapterConfig.setRealm(realm);
        keycloakAdapterConfig.setRealmKey(realmKey);
        keycloakAdapterConfig.setAuthServerUrl(authServerUrl);
        keycloakAdapterConfig.setSslRequired(sslRequired);
        keycloakAdapterConfig.setResource(resource);
        keycloakAdapterConfig.setPublicClient(publicClient);
        keycloakAdapterConfig.setCors(true);
        /*keycloakAdapterConfig.setTokenStore("cookie");*/

        keycloakAuthenticator.setAdapterConfig(keycloakAdapterConfig);
        securityHandler.setAuthenticator(keycloakAuthenticator);

        return securityHandler;
    }
}

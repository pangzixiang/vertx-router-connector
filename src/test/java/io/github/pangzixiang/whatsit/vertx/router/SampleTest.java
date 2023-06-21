package io.github.pangzixiang.whatsit.vertx.router;

import io.github.pangzixiang.whatsit.vertx.router.options.VertxRouterVerticleOptions;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleTest {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        SelfSignedCertificate selfSignedCertificate = SelfSignedCertificate.create();
        HttpServerOptions sslOptions = new HttpServerOptions().setSsl(true)
                .setKeyCertOptions(selfSignedCertificate.keyCertOptions())
                .setTrustOptions(selfSignedCertificate.trustOptions());
        vertx.deployVerticle(new VertxRouterVerticle(VertxRouterVerticleOptions.builder()
                        .proxyServerPort(8080)
                        .listenerServerPort(9090)
                        .proxyServerInstanceNumber(4)
                        .listenerServerOptions(sslOptions)
                        .proxyServerOptions(sslOptions)
                        .listenerServerInstanceNumber(4)
                        .enableBasicAuthentication(true)
                        .basicAuthenticationUsername("vertx-router")
                        .basicAuthenticationPassword("vertx-router-pwd").build()))
                .onSuccess(unused -> {
                    Router router = Router.router(vertx);
                    String serviceName = "junit-test-service";
                    router.route("/" + serviceName + "/test").handler(routingContext -> {
                        routingContext.response().end("test");
                    });

                    vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(0)
                            .onSuccess(httpServer -> {
                                VertxRouterConnectorVerticleOptions options = VertxRouterConnectorVerticleOptions
                                        .builder()
                                        .routerHost("localhost")
                                        .routerPort(9090)
                                        .registerURI("/register")
                                        .connectorHttpClientOptions(new HttpClientOptions().setSsl(true).setTrustAll(true))
                                        .serviceHost("localhost")
                                        .servicePort(httpServer.actualPort())
                                        .serviceName(serviceName)
                                        .optionalHeaders(MultiMap.caseInsensitiveMultiMap().add("Authorization", "Basic dmVydHgtcm91dGVyOnZlcnR4LXJvdXRlci1wd2Q="))
                                        .build();
                                vertx.deployVerticle(new VertxRouterConnectorVerticle(options)).onSuccess(id -> {
                                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                        vertx.undeploy(id);
                                    }));
                                });
                            })
                            .onFailure(throwable -> {
                                log.error("Failed to start up http server", throwable);
                            });
                });
    }
}

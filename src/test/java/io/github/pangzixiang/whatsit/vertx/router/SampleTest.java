package io.github.pangzixiang.whatsit.vertx.router;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SampleTest {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
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

    }
}

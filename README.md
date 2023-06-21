# Vertx Router Connector
#### how to use
For details, please refer to [vertx-router](https://github.com/pangzixiang/vertx-router)
- Target Services to use [vertx-router-connector](https://github.com/pangzixiang/vertx-router-connector) to connect
```xml
<dependency>
    <groupId>io.github.pangzixiang.whatsit</groupId>
    <artifactId>vertx-router-connector</artifactId>
    <version>{version}</version>
</dependency>
```
```java
public class Main {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);
        String serviceName = "test-service";
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
```

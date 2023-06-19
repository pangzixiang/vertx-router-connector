package io.github.pangzixiang.whatsit.vertx.router;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class VertxRouterConnectorVerticleOptions {
    private String routerHost;
    private Integer routerPort;
    private String registerURI;
    private String serviceHost;
    private Integer servicePort;
    private String serviceName;
    @Builder.Default
    private MultiMap optionalHeaders = MultiMap.caseInsensitiveMultiMap();
    @Builder.Default
    private HttpClientOptions connectorHttpClientOptions = new HttpClientOptions();
    @Builder.Default
    private Integer reconnectTimes = 5;
    @Builder.Default
    private Long reconnectDelay = 3_000L;
}

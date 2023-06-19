package io.github.pangzixiang.whatsit.vertx.router;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketConnectOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

@Slf4j
public class VertxRouterConnectorVerticle extends AbstractVerticle {
    private final VertxRouterConnectorVerticleOptions vertxRouterConnectorVerticleOptions;
    private HttpClient httpClient;

    public static final String VERTX_ROUTER_CONNECTOR_HEALTH_NAME = "vertx-router-connector-health-" + UUID.randomUUID();
    public static final String VERTX_ROUTER_CONNECTOR_HEALTH_STATUS = "STATUS";

    public VertxRouterConnectorVerticle(VertxRouterConnectorVerticleOptions vertxRouterConnectorVerticleOptions) {
        this.vertxRouterConnectorVerticleOptions = vertxRouterConnectorVerticleOptions;
    }

    @Override
    public void start() throws Exception {
        getVertx().sharedData().getLocalMap(VERTX_ROUTER_CONNECTOR_HEALTH_NAME).put(VERTX_ROUTER_CONNECTOR_HEALTH_STATUS, false);
        if (StringUtils.isAnyEmpty(vertxRouterConnectorVerticleOptions.getRouterHost(), vertxRouterConnectorVerticleOptions.getRegisterURI(),
                vertxRouterConnectorVerticleOptions.getServiceHost(), vertxRouterConnectorVerticleOptions.getServiceName()) ||
                ObjectUtils.anyNull(vertxRouterConnectorVerticleOptions.getRouterPort(), vertxRouterConnectorVerticleOptions.getServicePort())) {
            String err = "missing mandatory options!";
            log.error(err);
            return;
        }
        httpClient = getVertx().createHttpClient(vertxRouterConnectorVerticleOptions.getConnectorHttpClientOptions());
        WebSocketConnectOptions webSocketConnectOptions = new WebSocketConnectOptions();
        webSocketConnectOptions.setHost(vertxRouterConnectorVerticleOptions.getRouterHost());
        webSocketConnectOptions.setPort(vertxRouterConnectorVerticleOptions.getRouterPort());
        webSocketConnectOptions.setURI(vertxRouterConnectorVerticleOptions.getRegisterURI());
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("host", vertxRouterConnectorVerticleOptions.getServiceHost());
        headers.add("port", String.valueOf(vertxRouterConnectorVerticleOptions.getServicePort()));
        headers.add("name", vertxRouterConnectorVerticleOptions.getServiceName());
        headers.addAll(vertxRouterConnectorVerticleOptions.getOptionalHeaders());
        webSocketConnectOptions.setHeaders(headers);
        this.connect(webSocketConnectOptions);
    }

    private void connect(WebSocketConnectOptions webSocketConnectOptions) {
        getVertx().setPeriodic(0, vertxRouterConnectorVerticleOptions.getReconnectDelay(), l -> {
            log.info("Start to connect to vertx router {}:{}{}", vertxRouterConnectorVerticleOptions.getRouterHost(),
                    vertxRouterConnectorVerticleOptions.getRouterPort(), vertxRouterConnectorVerticleOptions.getRegisterURI());
            httpClient.webSocket(webSocketConnectOptions).onSuccess(webSocket -> {
                getVertx().cancelTimer(l);
                log.info("Succeeded to connect to vertx router {}:{}{}", vertxRouterConnectorVerticleOptions.getRouterHost(),
                        vertxRouterConnectorVerticleOptions.getRouterPort(), vertxRouterConnectorVerticleOptions.getRegisterURI());
                getVertx().sharedData().getLocalMap(VERTX_ROUTER_CONNECTOR_HEALTH_NAME).put(VERTX_ROUTER_CONNECTOR_HEALTH_STATUS, true);
                webSocket.closeHandler(unused -> {
                    getVertx().sharedData().getLocalMap(VERTX_ROUTER_CONNECTOR_HEALTH_NAME).put(VERTX_ROUTER_CONNECTOR_HEALTH_STATUS, false);
                    if (Objects.nonNull(httpClient)) {
                        connect(webSocketConnectOptions);
                    }
                });
            }).onFailure(throwable -> {
                log.error("Failed to connect to vertx router {}:{}{}", vertxRouterConnectorVerticleOptions.getRouterHost(),
                        vertxRouterConnectorVerticleOptions.getRouterPort(), vertxRouterConnectorVerticleOptions.getRegisterURI(), throwable);
            });
        });
    }

    @Override
    public void stop() throws Exception {
        if (Objects.nonNull(httpClient)) httpClient.close();
        if (Objects.nonNull(getVertx())) getVertx().sharedData().getLocalMap(VERTX_ROUTER_CONNECTOR_HEALTH_NAME).put(VERTX_ROUTER_CONNECTOR_HEALTH_STATUS, false);
        log.info("Closed connection to vertx router {}:{}{}", vertxRouterConnectorVerticleOptions.getRouterHost(),
                vertxRouterConnectorVerticleOptions.getRouterPort(), vertxRouterConnectorVerticleOptions.getRegisterURI());
    }
}

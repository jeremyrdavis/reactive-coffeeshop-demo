package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class DashboardVerticle extends AbstractVerticle {


  @Override
  public void start(Future<Void> startFuture) {

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

    BridgeOptions options = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("dashboard"))
      .addOutboundPermitted(new PermittedOptions().setAddress("dashboard"));
    sockJSHandler.bridge(options);

    Router router = Router.router(vertx);
    router.route("/queue/*").handler(sockJSHandler);


    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });
/*
      EventBus eventBus = vertx.eventBus();
      SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

      BridgeOptions options = new BridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddress("dashboard"))
        .addOutboundPermitted(new PermittedOptions().setAddress("dashboard"));
      sockJSHandler.bridge(options);

      Router router = Router.router(vertx);
//      router.route("/queue/*").handler(sockJSHandler);

      vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(8080, result -> {
          if (result.succeeded()) {
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });

*/
  }

}

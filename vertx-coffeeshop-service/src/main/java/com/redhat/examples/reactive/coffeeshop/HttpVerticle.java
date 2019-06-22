package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpVerticle extends AbstractVerticle {


  @Override
  public void start(Future<Void> startFuture) {

    // initialize the router
    Router baseRouter = Router.router(vertx);
    baseRouter.get("/").handler(this::rootHandler);
    baseRouter.route("/messaging").handler(BodyHandler.create());
    baseRouter.post("/messaging").handler(this::messagingHandler);

    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          startFuture.complete();
        } else {
          startFuture.fail(result.cause());
        }
      });

  }

  private void messagingHandler(RoutingContext routingContext) {
    MultiMap formAttributes = routingContext.request().formAttributes();
    String name = formAttributes.get("name");
    String beverage = formAttributes.get("beverage");
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("name:" + name + " order:" + beverage);
  }

  private void rootHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
  }
}

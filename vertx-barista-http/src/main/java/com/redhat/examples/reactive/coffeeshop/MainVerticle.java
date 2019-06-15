package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

public class MainVerticle extends AbstractVerticle {

  private String name;

  Router baseRouter = Router.router(vertx);

  @Override
  public void start(Future<Void> startFuture) {

    // Get the name for our Barista
    name = config().getString("name");

    // Handle the root url
    baseRouter.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Welcome to the Reactive Coffeeshop with Eclipse Vert.x!");
    });

    // Handle the barista functions
    baseRouter.route("/barista").handler(this::baristaHandler);

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

  private void baristaHandler(RoutingContext routingContext) {
    // fail for now
    routingContext.response().setStatusCode(500);
  }

}

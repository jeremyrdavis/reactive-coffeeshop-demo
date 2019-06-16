package com.redhat.examples.reactive.coffeeshop;


import io.reactivex.Maybe;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

  private String name;

  @Override
  public void start(final Future<Void> startFuture) {
    this.createHttpServer()
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
  }

  private void baristaHandler(RoutingContext routingContext) {
    // fail for now
    routingContext.response().setStatusCode(500);
  }

  /*
   * Create an HttpServer with the appropriate routes
   */
  private Maybe<HttpServer> createHttpServer() {

    this.name = config().getString("name", "Godzilla");

    // Create an instance of Router
    Router baseRouter = Router.router(vertx);

    // Handle the root url
    baseRouter.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain").end("Welcome to the Reactive Coffeeshop with Eclipse Vert.x!");
    });

    // Handle the barista functions
    baseRouter.post("/barista").handler(this::baristaHandler);

    return vertx.createHttpServer()
      .requestHandler(baseRouter::accept).rxListen().toMaybe();
  }

}

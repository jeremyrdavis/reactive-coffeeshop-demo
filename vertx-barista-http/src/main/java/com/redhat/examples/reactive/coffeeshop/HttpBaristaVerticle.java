package com.redhat.examples.reactive.coffeeshop;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class HttpBaristaVerticle extends AbstractVerticle{

  private static final Logger LOG = LoggerFactory.getLogger(HttpBaristaVerticle.class);

  private String name;

  private Random random = new Random();

  @Override
  public void start(final Future<Void> startFuture) {
    this.createHttpServer()
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
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
    baseRouter.route("/barista*").handler(BodyHandler.create());
    baseRouter.get("/barista").handler(this::baristaHandler);
    baseRouter.post("/barista").handler(this::orderHandler);

    return vertx.createHttpServer()
      .requestHandler(baseRouter::accept).rxListen().toMaybe();
  }

  /*
    Handler for posting an order
   */
  private void orderHandler(RoutingContext routingContext) {

    LOG.debug("orderHandler called with " + routingContext.getBody());

    Observable.zip(
      makeIt(
        new Order(
          routingContext.request().formAttributes().get("product"),
          routingContext.request().formAttributes().get("name"))),
      Observable.interval(random.nextInt(5) * 1000, TimeUnit.MILLISECONDS),
      (obs, timer) -> obs).doOnNext(beverage -> {
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "application/json").end(Json.encode(beverage));
      }
    ).subscribe();
  }

  /*
    Handler to display the name of the Barista
   */
  private void baristaHandler(RoutingContext routingContext) {

    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("Welcome to the Reactive Coffeeshop, I'm " + name);
  }

  private Observable<Beverage> makeIt(Order order) {
    return Single.just(new Beverage(order, name)).toObservable();
  }

}

package com.redhat.examples.reactive.coffeeshop;


import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static io.vertx.ext.web.handler.BodyHandler.*;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

  private String name;

  private Random random = new Random();

  @Override
  public void start(final Future<Void> startFuture) {
    this.loadConfig()
      .flatMap(this::createHttpServer)
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
  }

  /*
      Load our configuration file from a file in the classpath named, 'application-conf.json'
   */
  private Maybe<JsonObject> loadConfig() {

    ConfigStoreOptions fileStore = new ConfigStoreOptions()
      .setType("file")
      .setOptional(true)
      .setConfig(new JsonObject().put("path", "application-conf.json"));
    ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(fileStore);
    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    return retriever.rxGetConfig().toMaybe();

  }

  /*
   * Create an HttpServer with the appropriate routes
   */
  private Maybe<HttpServer> createHttpServer(JsonObject jsonConfig) {

    this.name = jsonConfig.getString("name", "Godzilla");

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

  private Observable<Beverage> makeIt(Order order) {
    return Single.just(new Beverage(order, name)).toObservable();
  }

  /*
    Handler for posting an order
   */
  private void orderHandler(RoutingContext routingContext) {

    LOG.debug("orderHandler");
    LOG.debug(routingContext.getBody());
    System.out.println("body: " + routingContext.getBodyAsString());
    JsonObject postBody = routingContext.getBodyAsJson();

    Observable.zip(
      makeIt(new Order(postBody.getString("product"), postBody.getString("name"), postBody.getString("orderId").toString())),
      Observable.interval(random.nextInt(5) * 1000, TimeUnit.MILLISECONDS),
      (obs, timer) -> obs).doOnNext(beverage -> {
        System.out.println(beverage.toString());
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

}

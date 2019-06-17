package com.redhat.examples.reactive.coffeeshop;


import io.reactivex.Maybe;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
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
    this.loadConfig()
      .flatMap(this::createHttpServer)
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
  }

  private void baristaHandler(RoutingContext routingContext) {

    // fail for now
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("Welcome to the Reactive Coffeeshop, I'm " + name );
  }

  /*
    Load our configuration file
   */
  private Maybe<JsonObject> loadConfig(){

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
    baseRouter.get("/barista").handler(this::baristaHandler);

    return vertx.createHttpServer()
      .requestHandler(baseRouter::accept).rxListen().toMaybe();
  }

}

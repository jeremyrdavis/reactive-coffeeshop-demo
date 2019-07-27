package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class SpikeHttpBarista extends AbstractVerticle {

  WebClient webClient;

  @Override
  public void start(Future<Void> startFuture) {

    CompositeFuture.all(
      initWebClient(),
      initHttpServer()).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });

  }


  private Future<Void> initWebClient() {
    Future<Void> initWebClientFuture = Future.future();
    try {
      webClient = WebClient.create(vertx);
      initWebClientFuture.complete();
    } catch (Exception e) {
      initWebClientFuture.fail(e.getCause());
    }
    return initWebClientFuture;
  }

  private Future<Void> initHttpServer() {

    Future<Void> initHttpServerFuture = Future.future();

    // initialize the router
    Router baseRouter = Router.router(vertx);
    baseRouter.get("/*").handler(StaticHandler.create());
    baseRouter.route("/http").handler(BodyHandler.create());
    baseRouter.post("/http").handler(this::httpHandler);
    baseRouter.route("/messaging").handler(BodyHandler.create());
    baseRouter.post("/messaging").handler(this::messagingHandler);

    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          initHttpServerFuture.complete();
        } else {
          initHttpServerFuture.fail(result.cause());
        }
      });
    return initHttpServerFuture;
  }

  private void messagingHandler(RoutingContext routingContext) {
    JsonObject requestJson = routingContext.getBodyAsJson();

    System.out.println("Coffeshop Service messagingHandler");
    System.out.println(requestJson.getString("name"));
    System.out.println(requestJson.getString("product"));

    JsonObject payload = new JsonObject()
      .put("name", requestJson.getString("name"))
      .put("product", requestJson.getString("product"));

    payload.put("action", "order-received");

    vertx.<JsonObject>eventBus().send("kafka-address", payload, ar -> {

      if (ar.succeeded()) {
        routingContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(payload));
      }else{
        routingContext.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(ar.cause().getMessage()));
      }
    });
  }

  private void httpHandler(RoutingContext routingContext) {
    JsonObject requestJson = routingContext.getBodyAsJson();

    System.out.println("Coffeshop Service httpHandler");
    System.out.println(requestJson.getString("name"));
    System.out.println(requestJson.getString("product"));

    JsonObject payload = new JsonObject()
      .put("name", requestJson.getString("name"))
      .put("product", requestJson.getString("product"));

    webClient.post(8082, "localhost", "/barista")
      .putHeader("Accept", "application/json")
      .sendJsonObject(payload, ar -> {
        if (ar.succeeded()) {
          HttpServerResponse response = routingContext.response();
          response.setStatusCode(200);
          response.putHeader("Content-type", "application/json").end(ar.result().bodyAsJsonObject().encode());
        }else{
          HttpServerResponse response = routingContext.response();
          response.setStatusCode(500);
          response.end();
        }
      });
  }
}

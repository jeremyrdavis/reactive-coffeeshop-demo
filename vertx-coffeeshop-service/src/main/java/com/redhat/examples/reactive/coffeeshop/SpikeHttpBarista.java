package com.redhat.examples.reactive.coffeeshop;

import com.redhat.examples.reactive.coffeeshop.model.Order;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

    BridgeOptions options = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("dashboard"))
      .addOutboundPermitted(new PermittedOptions().setAddress("dashboard"));
    sockJSHandler.bridge(options);

    // initialize the router
    Router baseRouter = Router.router(vertx);
    baseRouter.get("/*").handler(StaticHandler.create());
    baseRouter.route("/http").handler(BodyHandler.create());
    baseRouter.post("/http").handler(this::httpHandler);
    baseRouter.route("/messaging").handler(BodyHandler.create());
    baseRouter.post("/messaging").handler(this::messagingHandler);
    baseRouter.route("/queue/*").handler(sockJSHandler);

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
      .put("product", requestJson.getString("product"))
      .put("action", "order-received");

    vertx.<JsonObject>eventBus().send("kafka-address", payload, ar -> {

      if (ar.succeeded()) {
        vertx.eventBus().send("dashboard", payload);
        routingContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(payload));
      } else {
        routingContext.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json; charset=utf-8")
          .end(Json.encodePrettily(ar.cause().getMessage()));
      }
    });
  }

  private void httpHandler(RoutingContext routingContext) {

    System.out.println("Coffeshop Service httpHandler");
    Order order = Json.decodeValue(routingContext.getBody(), Order.class);

    System.out.println(order.toString());

    webClient.post(8088, "localhost", "/barista")
      .putHeader("Accept", "application/json")
      .sendJsonObject(order.toJsonObject(), ar -> {

        if (ar.succeeded()) {

          JsonObject result = ar.result().bodyAsJsonObject();

          JsonObject dashboard = new JsonObject()
            .put("beverage", result.getString("beverage"))
            .put("customer", result.getString("customer"))
            .put("preparedBy", result.getString("preparedBy"))
            .put("orderId", result.getString("orderId"));

          System.out.println("sending to dashboard" + dashboard.encode());
          vertx.eventBus().send("dashboard", dashboard);

          HttpServerResponse response = routingContext.response();
          response.setStatusCode(200);
          response.putHeader("Content-type", "application/json").end(ar.result().bodyAsJsonObject().encode());
        } else {
          System.out.println("cause: " + ar.cause());
          HttpServerResponse response = routingContext.response();
          response.setStatusCode(500);
          response.end();
        }
      });
  }
}

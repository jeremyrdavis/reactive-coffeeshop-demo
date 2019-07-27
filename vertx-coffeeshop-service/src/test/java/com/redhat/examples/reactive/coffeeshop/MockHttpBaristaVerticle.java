package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MockHttpBaristaVerticle extends AbstractVerticle {

  @Override
  public void start() {

    Router router = Router.router(vertx);
    router.route("/barista").handler(BodyHandler.create());
    router.post("/barista").handler(this::mockHandler);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8082, result -> {
        System.out.println("MockHttpBaristaVerticle deployed");
        if (result.failed()) {
          throw new RuntimeException(result.cause());
        }
      });

  }

  private void mockHandler(RoutingContext routingContext) {

    JsonObject payload = routingContext.getBodyAsJson();

    System.out.println("MockHttpBaristaVerticle");
    System.out.println(payload.getString("name"));
    System.out.println(payload.getString("product"));

    if(payload.getString("name") == null) throw new RuntimeException("'name' is null");
    if(payload.getString("product") == null) throw new RuntimeException("'product' is null");

    // if everything was sent correctly return the expected response
    HttpServerResponse response = routingContext.response();
    response.putHeader("Content-type", "application/json")
      .end(payload.encode());
  }

}

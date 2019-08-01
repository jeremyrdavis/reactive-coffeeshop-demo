package com.redhat.examples.reactive.coffeeshop;

import com.redhat.examples.reactive.coffeeshop.model.Order;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
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
      .listen(8088, result -> {
        System.out.println("MockHttpBaristaVerticle deployed");
        if (result.failed()) {
          throw new RuntimeException(result.cause());
        }
      });

  }

  private void mockHandler(RoutingContext routingContext) {

    JsonObject payload = routingContext.getBodyAsJson();

    Order order = Json.decodeValue(routingContext.getBody(), Order.class);

    System.out.println("MockHttpBaristaVerticle");
    System.out.println(order);

    if(order.getName() == null) throw new RuntimeException("'name' is null");
    if(order.getProduct() == null) throw new RuntimeException("'product' is null");

    // if everything was sent correctly return the expected response
    HttpServerResponse response = routingContext.response();
    response.putHeader("Content-type", "application/json")
      .end(payload.encode());
  }

}

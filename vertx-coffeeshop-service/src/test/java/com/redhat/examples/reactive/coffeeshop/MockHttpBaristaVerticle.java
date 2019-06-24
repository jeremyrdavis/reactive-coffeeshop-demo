package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.http.HttpServerResponse;

public class MockHttpBaristaVerticle extends AbstractVerticle {

  @Override
  public void start() {

    Router router = Router.router(vertx);
    router.route("/barista").handler(BodyHandler.create());
    router.post("/barista").handler(this::mockHandler);

    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(8082, result -> {
        if (result.failed()) {
          throw new RuntimeException(result.cause());
        }
      });

  }

  private void mockHandler(RoutingContext routingContext) {
    String product = routingContext.request().formAttributes().get("product");
    String name = routingContext.request().formAttributes().get("name");

    if(name == null) throw new RuntimeException("'name' is null");
    if(!(name.equals("Buffy"))) throw new RuntimeException("'name' does not equal 'Buffy'");
    if(product == null) throw new RuntimeException("'product' is null");
    if(!(product.equals("Venti Dark Roast"))) throw new RuntimeException("'product' does not equal 'Venti Dark Roast'");

    // if everything was sent correctly return the expected response
    HttpServerResponse response = routingContext.response();
    response.putHeader("Content-type", "application/json")
      .end(new JsonObject()
      .put("name", name)
      .put("beverage", product).encode());
  }

}

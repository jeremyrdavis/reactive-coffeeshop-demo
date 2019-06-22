package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {
    vertx.deployVerticle(HttpVerticle.class.getName(), ar -> {
      if (ar.failed()) {
        startFuture.fail(ar.cause());
      }else {
        startFuture.complete();
      }
    });
  }

}

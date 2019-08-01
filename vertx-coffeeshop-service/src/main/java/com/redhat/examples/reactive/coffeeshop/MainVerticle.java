package com.redhat.examples.reactive.coffeeshop;

import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {

    deployVerticle(SpikeHttpBarista.class.getName())
      .flatMap(id -> deployVerticle(KafkaVerticle.class.getName()))
      .subscribe(id -> startFuture.complete(), startFuture::fail);
  }

  private Single<String> deployVerticle(String verticleName) {
    return vertx.rxDeployVerticle(verticleName);
  }


}

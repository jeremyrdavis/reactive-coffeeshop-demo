package com.redhat.examples.reactive.coffeeshop;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.Future;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {

    deployVerticle(SpikeHttpBarista.class.getName())
      .doOnError(startFuture::fail)
      .subscribe(id -> startFuture.complete(), startFuture::fail);
  }

  private Maybe<String> deployVerticle(String verticleName) {
    return vertx.rxDeployVerticle(verticleName).toMaybe();
  }


}

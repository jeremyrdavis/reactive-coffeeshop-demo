package com.redhat.examples.reactive.coffeeshop;


import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(final Future<Void> startFuture) {
    this.loadConfig()
      .flatMap(this::configureDeployment)
      .flatMap(this::deployBarista)
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
  }

  private Maybe<String> deployBarista(DeploymentOptions deploymentOptions) {
    return vertx.rxDeployVerticle(HttpBaristaVerticle.class.getName(), deploymentOptions).toMaybe();
  }

  private Maybe<DeploymentOptions> configureDeployment(JsonObject jsonObject) {
    DeploymentOptions opts = new DeploymentOptions();
    opts.setConfig(jsonObject);
    return Single.just(opts).toMaybe();
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
}

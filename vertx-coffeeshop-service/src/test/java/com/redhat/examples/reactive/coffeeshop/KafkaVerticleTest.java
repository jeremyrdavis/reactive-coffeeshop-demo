package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class KafkaVerticleTest {

  @Test
  public void testOrderReceived(Vertx vertx, VertxTestContext tc) {

    vertx.deployVerticle(new KafkaVerticle(), tc.completing());

    JsonObject message = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast")
      .put("action", "order-received");

    JsonObject expected = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast")
      .put("action", "order-queued");

    vertx.<JsonObject>eventBus().send("kafka-address", message, ar -> {

      if (ar.succeeded()) {
        assertThat(ar.result().body()).isEqualTo(expected);
      }else {
        fail(ar.cause().getMessage());
      }
      tc.completeNow();
    });

  }
}

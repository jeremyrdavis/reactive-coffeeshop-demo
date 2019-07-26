package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class OrderTest {
  private Vertx vertx;

  @Test
  @DisplayName("Order Test")
  public void testPlacingAnOrder(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    JsonObject payload = new JsonObject().put("name", "Buffy").put("beverage", "latte");

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      deploymentCheckpoint.flag();

      webClient.post(8088, "localhost", "/barista")
        .sendJsonObject(payload, tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.getHeader("Content-Type").equals("application/json"));
            assertThat(resp.bodyAsString()).contains("Buffy");
            System.out.println(resp.bodyAsString());
            requestCheckpoint.flag();
          });
        }));
    }));

  }
}

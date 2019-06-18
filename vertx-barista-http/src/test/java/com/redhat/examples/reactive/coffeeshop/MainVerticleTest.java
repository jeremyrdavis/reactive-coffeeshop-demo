package com.redhat.examples.reactive.coffeeshop;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.*;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest {

  private Vertx vertx;

  @Test
  @DisplayName("Test Server Starts")
  public void testThatTheServerIsStarted(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      deploymentCheckpoint.flag();

      webClient.get(80, "localhost", "/")
        .as(BodyCodec.string())
        .send(tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.body()).contains("Welcome to the Reactive Coffeeshop with Eclipse Vert.x!");
            assertThat(resp.statusCode()).isEqualTo(200);
            requestCheckpoint.flag();
          });
        }));
    }));

  }

  @Test
  @DisplayName("Test Ordering a Beverage")
  public void testOrderingABeverage(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    JsonObject postBody = new JsonObject()
      .put("name", "Jeremy")
      .put("product", "Latte")
      .put("orderId", "1234567");

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      deploymentCheckpoint.flag();

      webClient.post(80, "localhost", "/barista")
        .as(BodyCodec.string())
        .putHeader("Content-Type", "application/json")
        .sendJsonObject(postBody, tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.body()).isNotNull();
            assertThat(resp.body()).contains("Jeremy");
            assertThat(resp.body()).contains("Igor");
            assertThat(resp.body()).contains("Latte");
            assertThat(resp.body()).contains("123456");
            requestCheckpoint.flag();
          });
        }));
    }));
  }

}

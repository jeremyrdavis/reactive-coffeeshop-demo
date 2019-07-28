package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class DashboardVerticleUnitTest {

  private Vertx vertx;

  @Test
  @DisplayName("Test Board Event Bus")
  public void testHttpEndpoint(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    JsonObject testPayload = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast");

    // deploy our mock verticle
    vertx.deployVerticle(MockHttpBaristaVerticle.class.getName());

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      // verify verticle is deployed
      deploymentCheckpoint.flag();

      webClient
        .post(8080, "localhost", "/http")
        .sendJsonObject(testPayload, tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.bodyAsString()).contains("Buffy");
            requestCheckpoint.flag();
          });
        }));
    }));
  }
}

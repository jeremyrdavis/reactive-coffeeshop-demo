package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class MessagingEndpointTest {

  @Test
  @DisplayName("Test Messaging Endpoint")
  public void testMessagingEndpoint(Vertx vertx, VertxTestContext tc) {

    JsonObject expectedMessage = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast")
      .put("action", "order-received");

    vertx.deployVerticle(new MockKafkaVerticle(expectedMessage), tc.completing());

    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    WebClient webClient = WebClient.create(vertx);
    System.out.println("WebClient created");

    JsonObject testPayload = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast");

    vertx.deployVerticle(new SpikeHttpBarista(), tc.succeeding(id -> {

      System.out.println("SpikeHttpBarista deployed");
      deploymentCheckpoint.flag();

      webClient.post(8080, "localhost", "/messaging")
        .sendJsonObject(testPayload, tc.succeeding(resp -> {
          System.out.println("resp:" + resp.bodyAsString());
          tc.verify(() -> {
            System.out.println("result: " + resp.bodyAsString());
            assertThat(resp.statusCode()).isEqualTo(200);
            assertThat(resp.bodyAsString()).contains("Buffy");
            requestCheckpoint.flag();
          });
        }));
    }));
  }
}

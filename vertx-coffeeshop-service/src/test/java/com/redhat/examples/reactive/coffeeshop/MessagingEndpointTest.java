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
public class MessagingEndpointTest {

  @Test
  @DisplayName("Test Messaging Endpoint")
  public void testHttpEndpoint(Vertx vertx, VertxTestContext tc) {

    vertx.deployVerticle(MockKafkaVerticle.class.getName());

    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    WebClient webClient = WebClient.create(vertx);
    System.out.println("WebClient created");

    JsonObject testPayload = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast");

    vertx.deployVerticle(new SpikeHttpBarista(), tc.succeeding(id -> {

      System.out.println("HttpVerticle deployed");
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

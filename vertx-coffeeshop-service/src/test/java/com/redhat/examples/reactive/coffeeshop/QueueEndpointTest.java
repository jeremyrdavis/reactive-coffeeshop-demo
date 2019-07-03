package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class QueueEndpointTest {

  private Vertx vertx;

  @Test
  @Timeout(5000)
  @DisplayName("Test Queue Endpoint")
  public void testQueueEndpoint(Vertx vertx, VertxTestContext tc) {

    WebClient webClient = WebClient.create(vertx);
    Checkpoint deploymentCheckpoint = tc.checkpoint();
    Checkpoint requestCheckpoint = tc.checkpoint();

    vertx.deployVerticle(new MainVerticle(), tc.succeeding(id -> {

      deploymentCheckpoint.flag();

      webClient.get(8080, "localhost", "/queue")
        .as(BodyCodec.string())
        .send(tc.succeeding(resp -> {
          tc.verify(() -> {
            assertThat(resp.body()).contains("Welcome to SockJS!");
            assertThat(resp.statusCode()).isEqualTo(200);
            requestCheckpoint.flag();
          });
        }));
    }));
  }
}

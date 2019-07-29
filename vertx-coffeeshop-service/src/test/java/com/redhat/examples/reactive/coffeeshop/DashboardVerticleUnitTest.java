package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class DashboardVerticleUnitTest {

  static final String QUEUE ="localhost:8080/queue";

  private static HttpClient httpClient;

  private Vertx vertx;

  @BeforeAll
  public static void setUp(Vertx vertx, VertxTestContext testContext){
    httpClient = vertx.createHttpClient();
  }


  @Test
  @DisplayName("Test SockJS")
  public void testSockJSData(Vertx vertx, VertxTestContext testContext) {

    Checkpoint requestCheckpoint = testContext.checkpoint();
    Checkpoint websocketCheckpoint = testContext.checkpoint();

    vertx.deployVerticle(new SpikeHttpBarista(), testContext.completing());

    // establish websocket connection
    httpClient.websocket(8080, "localhost", "/queue", websocket -> {
      websocket.handler(data -> {
        System.out.println("Received data " + data.toString("ISO-8859-1"));
        httpClient.close();
        websocketCheckpoint.flag();
      });
    });

    // data to send
    JsonObject testPayload = new JsonObject()
      .put("name", "Buffy")
      .put("product", "Venti Dark Roast");

    // send data to the endpoint
    HttpClientRequest httpClientRequest = httpClient.post(8080, "localhost", "/http", resp ->{
      System.out.println(resp.statusCode());
      requestCheckpoint.flag();
    });

    httpClientRequest.putHeader("content-type", "application/json");
    httpClientRequest.write(testPayload.encode());
    httpClientRequest.end();

  }
/*

  @Test
  @DisplayName("Test Server Starts")
  public void testRootUrl(Vertx vertx, VertxTestContext tc) {

    final HttpClient httpClient = vertx.createHttpClient();

    vertx.deployVerticle(new DashboardVerticle(), tc.completing());

    httpClient.getNow(8080, "localhost", "/queue", resp -> {
      resp.bodyHandler(body -> {
        System.out.println("handling");
        assertThat(body.toString()).contains("Hello, Vert.x!");
        tc.completeNow();
      });
    });

  }

*/
}

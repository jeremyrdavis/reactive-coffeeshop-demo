package com.redhat.examples.reactive.coffeeshop;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MockKafkaVerticle extends AbstractVerticle {

  JsonObject expectedMessage;

  public MockKafkaVerticle(JsonObject expectedMessage) {

    this.expectedMessage = expectedMessage;
  }

  @Override
  public void start(Future<Void> startFuture) {

    EventBus eventBus = vertx.eventBus();
    MessageConsumer<JsonObject> consumer = eventBus.consumer("kafka-address");

    consumer.handler(message -> {

      String action = message.body().getString("action");

      switch (action) {
        case "order-received":
          orderReceived(message);
          break;
        default:
          message.fail(1, "Unkown action: " + message.body());
      }
    });

    startFuture.complete();

  }

  private void orderReceived(Message<JsonObject> message) {
    assertThat(message.body()).isEqualTo(expectedMessage);
    message.reply(new JsonObject().put("result", "success"));
  }
}

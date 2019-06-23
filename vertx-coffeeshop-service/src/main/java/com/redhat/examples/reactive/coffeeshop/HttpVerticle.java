package com.redhat.examples.reactive.coffeeshop;

import com.redhat.examples.reactive.coffeeshop.model.Order;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HttpVerticle extends AbstractVerticle {

  private KafkaProducer<String, String> kafkaProducer;

  private Future<Void> testKafkaSendFuture;

  @Override
  public void start(Future<Void> startFuture) {

    CompositeFuture.all(
      initKafkaProducer(),
      initHttpServer()).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      }else{
        startFuture.fail(ar.cause());
      }
    });

  }

  private Future<Void> initHttpServer() {

    Future<Void> initHttpServerFuture = Future.future();

    // initialize the router
    Router baseRouter = Router.router(vertx);
    baseRouter.get("/").handler(this::rootHandler);
    baseRouter.route("/messaging").handler(BodyHandler.create());
    baseRouter.post("/messaging").handler(this::messagingHandler);

    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8081, result -> {
        if (result.succeeded()) {
          initHttpServerFuture.complete();
        } else {
          initHttpServerFuture.fail(result.cause());
        }
      });
    return initHttpServerFuture;
  }

  private void messagingHandler(RoutingContext routingContext) {

    Order order = new Order(
      routingContext.request().formAttributes().get("beverage"),
      routingContext.request().formAttributes().get("name"));
    order.setOrderId(UUID.randomUUID().toString());

    System.out.println(Json.encodePrettily(order));

    CompositeFuture.all(
      sendOrderToKafka(order, KafkaQueue.ORDERS),
      sendOrderToKafka(order, KafkaQueue.QUEUE)).setHandler(ar -> {
      if (ar.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(Json.encodePrettily(order));
      }else{
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(new String("{ \"error\":\"" + ar.cause() + "\"}"));
      }
    });

/*
    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create("orders", Json.encodePrettily(order));

    kafkaProducer.write(record, ar -> {
      if (ar.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(Json.encodePrettily(order));
        RecordMetadata recordMetadata = ar.result();
      }else{
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(new String("{ \"error\":\"" + ar.cause() + "\"}"));
      }
    });
*/

  }

  private void rootHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
  }

  private Future<Void> sendOrderToKafka(Order order, KafkaQueue kafaQueue){
    Future<Void> sendFuture = Future.future();
    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(kafaQueue.name, Json.encodePrettily(order));
    kafkaProducer.write(record, ar -> {
      if (ar.failed()) {
        sendFuture.fail(ar.cause());
      }else{
        sendFuture.complete();
      }
    });
    return sendFuture;
  }

  private Future<Void> initKafkaProducer() {
    Future<Void> initKafkaProducerFuture = Future.future();

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");

    try {
      kafkaProducer = KafkaProducer.create(vertx, config);
      initKafkaProducerFuture.complete();
    } catch (Exception e) {
      initKafkaProducerFuture.fail(e);
    }

    return initKafkaProducerFuture;
  }

  enum KafkaQueue{

  ORDERS("orders"), QUEUE("queue");

    public String name;

    private KafkaQueue(String name){
      this.name = name;
    }
  }

}

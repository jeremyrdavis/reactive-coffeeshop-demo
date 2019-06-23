package com.redhat.examples.reactive.coffeeshop;

import com.redhat.examples.reactive.coffeeshop.model.Order;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
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
        testKafkaSend().setHandler(handler -> {
          if (handler.failed()) {
            startFuture.fail(handler.cause());
          }else{
            startFuture.complete();
          }
        });
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
      routingContext.request().formAttributes().get("name"),
      routingContext.request().formAttributes().get("beverage"));
    order.setOrderId(UUID.randomUUID().toString());

    System.out.println(Json.encodePrettily(order));

    KafkaProducerRecord<String, String> record = KafkaProducerRecord.create("orders", Json.encodePrettily(order));

    kafkaProducer.write(record, ar -> {
      if (ar.succeeded()) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(Json.encodePrettily(order));
        RecordMetadata recordMetadata = ar.result();
        System.out.println("Message " + record.value() + " written on topic=" + recordMetadata.getTopic() +
          ", partition=" + recordMetadata.getPartition() +
          ", offset=" + recordMetadata.getOffset());
      }else{
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(new String("{ \"error\":\"" + ar.cause() + "\"}"));
      }
    });

//    HttpServerResponse response = routingContext.response();
//    response.putHeader("content-type", "application/json").end(Json.encodePrettily(order));
  }

  private void rootHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader("content-type", "text/plain").end("Hello Vert.x!");
  }

  private Future<Void> initKafkaProducer() {
    Future<Void> initKafkaProducerFuture = Future.future();

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");
/*
    config.put("auto.commit", "false");
    config.put("enable.auto.commit", "false");
*/

    try {
      kafkaProducer = KafkaProducer.create(vertx, config);
      initKafkaProducerFuture.complete();
    } catch (Exception e) {
      initKafkaProducerFuture.fail(e);
    }

    return initKafkaProducerFuture;
  }

  private Future<Void> testKafkaSend() {

    testKafkaSendFuture = Future.future();

    KafkaProducerRecord<String, String> record =
      KafkaProducerRecord.create("queue",
        new JsonObject()
          .put("id", 1)
          .put("name", "jeremy")
          .put("product", "latte").encodePrettily());

    kafkaProducer.write(record, ar->{
      if (ar.failed()) {
        testKafkaSendFuture.fail(ar.cause());
      }else{
        testKafkaSendFuture.complete();
      }
    });


    return testKafkaSendFuture;
  }
}

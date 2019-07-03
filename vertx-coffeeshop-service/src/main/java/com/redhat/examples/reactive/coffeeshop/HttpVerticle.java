package com.redhat.examples.reactive.coffeeshop;

import com.redhat.examples.reactive.coffeeshop.model.Order;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /http - POST relays an Order to the barista-http
 * /async - POST relays an Order to the
 */
public class HttpVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

  private KafkaProducer<String, String> kafkaProducer;

  private KafkaConsumer<String, String> kafkaConsumer;

  WebClient webClient;

  @Override
  public void start(Future<Void> startFuture) {

    CompositeFuture.all(
      initWebClient(),
      initKafkaProducer(),
      initKafkaConsumer(),
      initHttpServer()).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      }else{
        startFuture.fail(ar.cause());
      }
    });

  }

  private Future<Void> initWebClient() {
    Future<Void> initWebClientFuture = Future.future();
    try {
      webClient = WebClient.create(vertx);
      initWebClientFuture.complete();
    } catch (Exception e) {
      initWebClientFuture.fail(e.getCause());
    }
    return initWebClientFuture;
  }

  private Future<Void> initHttpServer() {

    Future<Void> initHttpServerFuture = Future.future();

    // initialize the router
    Router baseRouter = Router.router(vertx);
//    baseRouter.get("/").handler(this::rootHandler);
//    baseRouter.get("/").handler(StaticHandler.create("webroot"));
    baseRouter.get("/*").handler(StaticHandler.create());
//    baseRouter.get("/webroot").handler(StaticHandler.create());
    baseRouter.route("/messaging").handler(BodyHandler.create());
    baseRouter.post("/messaging").handler(this::messagingHandler);
    baseRouter.route("/http").handler(BodyHandler.create());
    baseRouter.post("/http").handler(this::httpHandler);

    EventBus eventBus = vertx.eventBus();
    BridgeOptions options = new BridgeOptions();
    baseRouter.route("/queue/*").handler(SockJSHandler.create(vertx).bridge(options));

/*
    // initialize a SockJSHandler
    SockJSHandlerOptions options = new SockJSHandlerOptions().setHeartbeatInterval(2000);
    SockJSHandler sockJSHandler = SockJSHandler
      .create(vertx, options)
      .bridge(new BridgeOptions(), event -> {
        if (event.type() == BridgeEventType.SOCKET_CREATED) {
          logger.info("A socket was created");
        }
        seek();
        event.complete(true);
      });
    sockJSHandler.socketHandler(sockJSSocket -> {
     // Just echo the data back
      sockJSSocket.handler(sockJSSocket::write);
    });
    // attache the SockJSHandler to the /queue route
    baseRouter.get("/queue").handler(sockJSHandler);
*/

    vertx.createHttpServer()
      .requestHandler(baseRouter::accept)
      .listen(8080, result -> {
        if (result.succeeded()) {
          initHttpServerFuture.complete();
        } else {
          initHttpServerFuture.fail(result.cause());
        }
      });
    return initHttpServerFuture;
  }

  private void rootHandler(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    response.putHeader("Content-type", "text/html")
      .sendFile("/webroot/index.html");
      //.end("Hello Vert.x!");
  }

  /*
    1. Get Json from the request
    2. Create a form to send to the http barista
    3. Call the http barista with the form
    4. Translate the response into Json
   */
  private void httpHandler(RoutingContext routingContext) {
    JsonObject requestJson = routingContext.getBodyAsJson();

    System.out.println(requestJson.getString("name"));
    System.out.println(requestJson.getString("product"));

    MultiMap form = MultiMap.caseInsensitiveMultiMap();
    form.set("name", requestJson.getString("name"));
    form.set("product", requestJson.getString("product"));

    webClient.post(8082, "localhost", "/barista")
      .putHeader("Accept", "application/json")
      .sendForm(form, ar -> {
        if (ar.succeeded()) {
          HttpServerResponse response = routingContext.response();
          response.setStatusCode(200);
          response.putHeader("Content-type", "application/json").end(ar.result().bodyAsJsonObject().encode());
        }else{
          HttpServerResponse response = routingContext.response();
          response.setStatusCode(500);
          response.end();
        }
      });
  }

  private void queueHandler(RoutingContext routingContext) {
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

  private void seek() {
    TopicPartition topicPartition = new TopicPartition()
      .setTopic("test")
      .setPartition(0);

// seek to a specific offset
    kafkaConsumer.seek(topicPartition, 10, done -> {
      if (done.succeeded()) {
        System.out.println("Seeking done");
      }
    });
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

  private Future<Void> initKafkaConsumer(){
    Future<Void> initKafkaConsumerFuture = Future.future();

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("acks", "1");

    try {
      kafkaConsumer = KafkaConsumer.create(vertx, config);
      kafkaConsumer.subscribe("queue");
      initKafkaConsumerFuture.complete();
    } catch (Exception e) {
      initKafkaConsumerFuture.fail(e);
    }

    return initKafkaConsumerFuture;

  }

  enum KafkaQueue{

  ORDERS("orders"), QUEUE("queue");

    public String name;

    private KafkaQueue(String name){
      this.name = name;
    }
  }

}

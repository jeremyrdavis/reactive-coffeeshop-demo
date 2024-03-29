package com.redhat.examples.reactive.coffeeshop;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.core.AbstractVerticle;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class KafkaBaristaVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaBaristaVerticle.class);

  private static final String ORDER_TOPIC = "orders";

  private KafkaConsumer<String, String> kafkaConsumer;

  private KafkaProducer<String, String> kafkaProducer;

  private String name;

  private Random random = new Random();

  @Override
  public void start(final Future<Void> startFuture) {

//    loadKafkaConfig()
//      .flatMap(this::initKafkaConsumer)
//      .flatMap(conf -> {
//        initKafkaConsumer(conf);
//      })
    initKafkaConsumer()
      .doOnError(startFuture::fail)
      .subscribe(v -> startFuture.complete());
  }

  private Observable<Map<String, String>> loadKafkaConfig(){

    System.out.println("loadKafkaConfig: " + config().getJsonObject("kafkaConfig"));
/*
    return Observable.just(config().getJsonObject("kafkaConfig").getMap())
      .map(stringObjectAMap -> stringObjectAMap.entrySet())
      .flatMapIterable(entries -> entries)
      .map(e -> new HashMap<String, String>().put(e.getKey(), e.getValue().toString()))
      .toMap(e -> e).toObservable()
      .doOnError(System.out::println);
*/

    Observable.just(config().getJsonObject("kafkaConfig").getMap())
      .map(objectMap -> objectMap.entrySet())
      .flatMapIterable(entries -> entries)
      .map(e -> new HashMap<String, String>().put(e.getKey(), e.getValue().toString()))
      .subscribe(System.out::println);
    return null;
  }

  private Observable<Void> initKafkaConsumer() {

    return Completable.fromRunnable(() -> {
      Map<String, Object> kafkaConf = config().getJsonObject("kafkaConfig").getMap();
      Map<String, String> kafkaConfig = new HashMap<String, String>(kafkaConf.size());
      kafkaConf.keySet().forEach(k -> {
        kafkaConfig.put(k, kafkaConf.get(k).toString());
      });
      kafkaConsumer = KafkaConsumer.create(vertx, kafkaConfig);
      kafkaConsumer.handler(record -> {
        System.out.println(record.value());
        Order order = Json.decodeValue(record.value(), Order.class);
        System.out.println("Order:" + order);
        System.out.println("Order Will be processed by " + this.name);

      });
      kafkaConsumer.subscribe(ORDER_TOPIC, ar ->{
        if (ar.succeeded()) {
          System.out.println("subscribed");
        } else {
          System.out.println("Could not subscribe " + ar.cause().getMessage());
        }
      });
    }).toObservable();
  }

  private Future<Void> initKafkaProducer() {
    Future<Void> initKafkaProducerFuture = Future.future();

    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("acks", "1");
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "coffeeshop");

    try {
      kafkaProducer = KafkaProducer.create((Vertx) vertx, config);
      initKafkaProducerFuture.complete();
    } catch (Exception e) {
      initKafkaProducerFuture.fail(e);
    }

    return initKafkaProducerFuture;
  }


}

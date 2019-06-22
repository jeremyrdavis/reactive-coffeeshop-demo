package com.redhat.examples.reactive.coffeeshop;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class KafkaBaristaVerticle extends AbstractVerticle {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaBaristaVerticle.class);

  private static final String ORDER_TOPIC = "orders";

  private KafkaConsumer<String, String> kafkaConsumer;

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
        System.out.println("Order Received key=" + record.key() + ",value=" + record.value() +
          ",partition=" + record.partition() + ",offset=" + record.offset());
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

}

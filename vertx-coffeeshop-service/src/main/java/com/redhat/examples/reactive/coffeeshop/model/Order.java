package com.redhat.examples.reactive.coffeeshop.model;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class Order {

  private String product;
  private String name;
  private UUID orderId;

  public Order() {
    super();
    this.orderId = UUID.randomUUID();
  }

  public Order(String product, String name) {
    this.product = product;
    this.name = name;
    this.orderId = UUID.randomUUID();
  }

  public JsonObject toJsonObject(){
    return new JsonObject()
      .put("product", this.product)
      .put("name", this.name)
      .put("orderId", this.orderId.toString());
  }


  public String getProduct() {
    return product;
  }

  public Order setProduct(String product) {
    this.product = product;
    return this;
  }

  public String getName() {
    return name;
  }

  public Order setName(String name) {
    this.name = name;
    return this;
  }

  public UUID getOrderId() {
    return orderId;
  }

}

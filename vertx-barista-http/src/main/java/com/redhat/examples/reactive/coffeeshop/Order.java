package com.redhat.examples.reactive.coffeeshop;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Order {

  private String product;
  private String name;
  private UUID orderId;

  public Order(String product, String name) {
    this.product = product;
    this.name = name;
    this.orderId = UUID.randomUUID();
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

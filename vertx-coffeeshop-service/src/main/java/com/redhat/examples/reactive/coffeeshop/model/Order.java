package com.redhat.examples.reactive.coffeeshop.model;

public class Order {

  private String product;
  private String name;
  private String orderId;

  public Order(String product, String name) {
    this.product = product;
    this.name = name;
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

  public String getOrderId() {
    return orderId;
  }

  public Order setOrderId(String orderId) {
    this.orderId = orderId;
    return this;
  }
}
/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.zeebe.tasklist.entities;

public abstract class TasklistEntity<T extends TasklistEntity<T>> {

  private String id;

  public String getId() {
    return id;
  }

  public T setId(String id) {
    this.id = id;
    return (T) this;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final TasklistEntity<T> that = (TasklistEntity<T>) o;

    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public String toString() {
    return "TasklistEntity{" + "id='" + id + "\'}";
  }
}

/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.db.impl.rocksdb.transaction;

import org.rocksdb.Transaction;

@FunctionalInterface
public interface TransactionRenovator {

  /**
   * Renews the given oldTransaction such that it can reused.
   *
   * @param oldTransaction the old transaction which becomes new
   * @return the renewed transaction
   */
  Transaction renewTransaction(Transaction oldTransaction);
}

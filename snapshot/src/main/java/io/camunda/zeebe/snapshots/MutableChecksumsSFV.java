/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.snapshots;

import java.io.IOException;
import java.nio.file.Path;

/** Mutable checksum collection in simple file verification (SFV) file format */
public interface MutableChecksumsSFV extends ImmutableChecksumsSFV {

  /**
   * Update the checksum collection, and add a new checksum from a given file path.
   *
   * @param filePath the path to a file for which a checksum is created and added to the collection
   * @throws IOException when reading of given file fails
   */
  void updateFromFile(final Path filePath) throws IOException;

  /**
   * Build the checksum collection from a SFV format string array.
   *
   * @param lines the lines (in SFV) to build up the checksum collection
   */
  void updateFromSfvFile(final String... lines);
}
/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.1. You may not use this file
 * except in compliance with the Zeebe Community License 1.1.
 */
package io.camunda.zeebe.backup.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.zeebe.backup.api.NamedFileSet;
import java.nio.file.Path;
import org.assertj.core.api.AbstractAssert;

final class NamedFileSetAssert extends AbstractAssert<NamedFileSetAssert, NamedFileSet> {

  private NamedFileSetAssert(final NamedFileSet namedFileSet, final Class<?> selfType) {
    super(namedFileSet, selfType);
  }

  public static NamedFileSetAssert assertThatNamedFileSet(NamedFileSet actual) {
    return new NamedFileSetAssert(actual, NamedFileSetAssert.class);
  }

  @SuppressWarnings("UnusedReturnValue")
  public NamedFileSetAssert hasSameContentsAs(NamedFileSet expected) {
    for (final var expectedEntry : expected.namedFiles().entrySet()) {
      final var expectedName = expectedEntry.getKey();
      final var expectedPath = expectedEntry.getValue();
      final var actualNamedFiles = actual.namedFiles();

      assertThat(actualNamedFiles).containsKey(expectedName);
      final var actualPath = actualNamedFiles.get(expectedEntry.getKey());
      assertThat(actualPath).hasSameBinaryContentAs(expectedPath);
    }
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public NamedFileSetAssert residesInPath(Path expectedPath) {
    assertThat(actual.files())
        .allSatisfy(
            actualPath -> {
              assertThat(actualPath).hasParent(expectedPath);
            });
    return this;
  }
}
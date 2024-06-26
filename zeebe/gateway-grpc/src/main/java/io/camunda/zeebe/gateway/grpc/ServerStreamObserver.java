/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.gateway.grpc;

import io.camunda.zeebe.gateway.impl.job.ResponseObserver;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * A simple extension of {@link StreamObserver}, meant to be used in conjunction with {@link
 * io.grpc.stub.ServerCallStreamObserver}. In order to avoid depending on {@link
 * io.grpc.stub.ServerCallStreamObserver}, which is experimental (as of now), we introduce this
 * simple interface, and we can easily change the implementation whenever the experimental API is
 * changed.
 *
 * @param <GrpcResponseT> the expected gRPC response type
 */
public interface ServerStreamObserver<GrpcResponseT>
    extends StreamObserver<GrpcResponseT>, ResponseObserver<GrpcResponseT> {
  /**
   * @see ServerCallStreamObserver#isCancelled()
   */
  @Override
  boolean isCancelled();

  /**
   * @see ServerCallStreamObserver#setOnCancelHandler(Runnable)
   */
  void setOnCancelHandler(final Runnable handler);
}

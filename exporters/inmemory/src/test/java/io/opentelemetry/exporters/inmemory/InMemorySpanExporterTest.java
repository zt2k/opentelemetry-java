/*
 * Copyright 2019, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.exporters.inmemory;

import static com.google.common.truth.Truth.assertThat;

import io.opentelemetry.sdk.trace.SpanData;
import io.opentelemetry.sdk.trace.TracerSdkRegistry;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter.ResultCode;
import io.opentelemetry.trace.Tracer;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InMemorySpanExporter}. */
@RunWith(JUnit4.class)
public class InMemorySpanExporterTest {
  private final TracerSdkRegistry tracerSdkRegistry = TracerSdkRegistry.create();
  private final Tracer tracer = tracerSdkRegistry.get("InMemorySpanExporterTest");
  private final InMemorySpanExporter exporter = InMemorySpanExporter.create();

  @Before
  public void setup() {
    tracerSdkRegistry.addSpanProcessor(SimpleSpansProcessor.newBuilder(exporter).build());
  }

  @Test
  public void getFinishedSpanItems() {
    tracer.spanBuilder("one").startSpan().end();
    tracer.spanBuilder("two").startSpan().end();
    tracer.spanBuilder("three").startSpan().end();

    List<SpanData> spanItems = exporter.getFinishedSpanItems();
    assertThat(spanItems).isNotNull();
    assertThat(spanItems.size()).isEqualTo(3);
    assertThat(spanItems.get(0).getName()).isEqualTo("one");
    assertThat(spanItems.get(1).getName()).isEqualTo("two");
    assertThat(spanItems.get(2).getName()).isEqualTo("three");
  }

  @Test
  public void reset() {
    tracer.spanBuilder("one").startSpan().end();
    tracer.spanBuilder("two").startSpan().end();
    tracer.spanBuilder("three").startSpan().end();
    List<SpanData> spanItems = exporter.getFinishedSpanItems();
    assertThat(spanItems).isNotNull();
    assertThat(spanItems.size()).isEqualTo(3);
    // Reset then expect no items in memory.
    exporter.reset();
    assertThat(exporter.getFinishedSpanItems()).isEmpty();
  }

  @Test
  public void shutdown() {
    tracer.spanBuilder("one").startSpan().end();
    tracer.spanBuilder("two").startSpan().end();
    tracer.spanBuilder("three").startSpan().end();
    List<SpanData> spanItems = exporter.getFinishedSpanItems();
    assertThat(spanItems).isNotNull();
    assertThat(spanItems.size()).isEqualTo(3);
    // Shutdown then expect no items in memory.
    exporter.shutdown();
    assertThat(exporter.getFinishedSpanItems()).isEmpty();
    // Cannot add new elements after the shutdown.
    tracer.spanBuilder("one").startSpan().end();
    assertThat(exporter.getFinishedSpanItems()).isEmpty();
  }

  @Test
  public void export_ReturnCode() {
    assertThat(exporter.export(Collections.singletonList(makeBasicSpan())))
        .isEqualTo(ResultCode.SUCCESS);
    exporter.shutdown();
    // After shutdown no more export.
    assertThat(exporter.export(Collections.singletonList(makeBasicSpan())))
        .isEqualTo(ResultCode.FAILED_NOT_RETRYABLE);
    exporter.reset();
    // Reset does not do anything if already shutdown.
    assertThat(exporter.export(Collections.singletonList(makeBasicSpan())))
        .isEqualTo(ResultCode.FAILED_NOT_RETRYABLE);
  }

  static SpanData makeBasicSpan() {
    return SpanData.newBuilder()
        .setTraceId(io.opentelemetry.trace.TraceId.getInvalid())
        .setSpanId(io.opentelemetry.trace.SpanId.getInvalid())
        .setName("span")
        .setKind(io.opentelemetry.trace.Span.Kind.SERVER)
        .setStartEpochNanos(100_000_000_100L)
        .setStatus(io.opentelemetry.trace.Status.OK)
        .setEndEpochNanos(200_000_000_200L)
        .build();
  }
}

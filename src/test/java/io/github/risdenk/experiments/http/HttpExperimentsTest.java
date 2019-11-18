package io.github.risdenk.experiments.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpExperimentsTest {
  @Test
  void testAdd() {
    Assertions.assertEquals(42, Integer.sum(19, 23));
  }
}

package co.edu.unbosque;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import co.edu.unbosque.marketplace.entity.CancellationPolicy;

class CancellationPolicyTest {

  @Test
  void flexible() {
    assertEquals(100, CancellationPolicy.FLEXIBLE.refundPercent(1));
    assertEquals(0, CancellationPolicy.FLEXIBLE.refundPercent(0));
  }

  @Test
  void moderate() {
    assertEquals(100, CancellationPolicy.MODERATE.refundPercent(5));
    assertEquals(50, CancellationPolicy.MODERATE.refundPercent(2));
    assertEquals(0, CancellationPolicy.MODERATE.refundPercent(0));
  }

  @Test
  void strict() {
    assertEquals(50, CancellationPolicy.STRICT.refundPercent(7));
    assertEquals(0, CancellationPolicy.STRICT.refundPercent(6));
  }
}

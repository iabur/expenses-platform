package com.expenses.svcfx.web;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/fx")
public class FxController {
  @GetMapping
  public Map<String, Object> getRates(@RequestParam String base, @RequestParam String date) {
    Map<String,Double> rates = Map.of("USD", 1.0, "BDT", 109.85, "EUR", 0.92);
    return Map.of("base", base, "date", date, "rates", rates);
  }
}

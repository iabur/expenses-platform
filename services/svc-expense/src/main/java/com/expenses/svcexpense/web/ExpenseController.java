package com.expenses.svcexpense.web;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
  @PostMapping
  public Map<String,Object> create(@RequestBody Map<String,Object> req) {
    Map<String,Object> res = new HashMap<>(req);
    res.put("id", UUID.randomUUID().toString());
    res.putIfAbsent("currency","BDT");
    return res;
  }

  @GetMapping("/{id}")
  public Map<String,Object> get(@PathVariable String id) {
    return Map.of("id", id, "amount", 25000, "currency", "BDT");
  }
}

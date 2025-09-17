package com.expenses.svcgroup.web;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/groups")
public class GroupController {
  @PostMapping
  public Map<String,Object> create(@RequestBody Map<String,Object> req) {
    return Map.of("id", UUID.randomUUID().toString(), "name", req.getOrDefault("name","My Group"), "defaultCurrency", req.getOrDefault("defaultCurrency","BDT"));
  }

  @GetMapping("/{id}")
  public Map<String,Object> get(@PathVariable String id) {
    return Map.of("id", id, "name", "Demo Group", "defaultCurrency", "BDT");
  }
}

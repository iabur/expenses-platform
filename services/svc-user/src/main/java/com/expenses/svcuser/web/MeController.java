package com.expenses.svcuser.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MeController {
  @GetMapping("/user/me")
  public Map<String,Object> me() {
    return Map.of("displayName","Alice Example","defaultCurrency","BDT");
  }
}

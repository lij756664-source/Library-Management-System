package com.example.book.controller;

import com.example.book.annotation.RequireRole;
import com.example.book.common.Result;
import com.example.book.service.RuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@RequireRole("ADMIN")
public class RuleController {
    private final RuleService service;

    public RuleController(RuleService service) {
        this.service = service;
    }

    @GetMapping("/latest")
    @RequireRole({"ADMIN", "USER"})
    public Result<Map<String, String>> latest() {
        return Result.success(service.current());
    }

    @GetMapping
    public Result<Map<String, String>> get() {
        return Result.success(service.current());
    }

    @PutMapping
    public Result<Map<String, String>> save(@RequestBody Map<String, String> values) {
        return Result.success(service.save(values));
    }
}

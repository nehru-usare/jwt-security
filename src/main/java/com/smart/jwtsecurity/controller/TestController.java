package com.smart.jwtsecurity.controller;

import org.springframework.web.bind.annotation.*;

/**
 * Protected endpoints.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/user")
    public String user() {
        return "USER ACCESS";
    }

    @GetMapping("/admin")
    public String admin() {
        return "ADMIN ACCESS";
    }
}

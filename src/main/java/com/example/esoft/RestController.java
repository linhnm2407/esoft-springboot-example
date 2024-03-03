package com.example.esoft;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@org.springframework.web.bind.annotation.RestController
@RequestMapping
public class RestController {

    @GetMapping("/hello")
    public String ping() {
        return "Hello World - Development Branch";
    }
}
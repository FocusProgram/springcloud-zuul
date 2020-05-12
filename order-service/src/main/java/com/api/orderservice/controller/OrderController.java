package com.api.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: Mr.Kong
 * @Date: 2020/5/12 17:01
 * @Description:
 */
@RestController
public class OrderController {

    @GetMapping("/demo")
    public String getDemo(){
        return "order";
    }

}

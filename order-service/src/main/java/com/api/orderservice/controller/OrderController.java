package com.api.orderservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: Mr.Kong
 * @Date: 2020/5/12 17:01
 * @Description:
 */
@RestController
@RequestMapping("order")
public class OrderController {

    @GetMapping("/getOrder")
    public String getOrder(){
        return "order";
    }

    @GetMapping("getFilter")
    public String getFilter(){
        return "filter";
    }

}

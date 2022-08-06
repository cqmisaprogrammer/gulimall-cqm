package com.cqm.gulimall.order.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HelloController {

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page")String page){
        System.out.println(page);
        return page;

    }
}

package me.ningyu.app.hostify.facade.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController
{
    /**
     * 根路径重定向到管理页面
     */
    @GetMapping("/")
    public String home()
    {
        return "forward:/index.html";
    }
}
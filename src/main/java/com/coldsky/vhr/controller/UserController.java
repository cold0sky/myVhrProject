package com.coldsky.vhr.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @RequestMapping(value = { "", "/index" }, method = RequestMethod.GET)
    public String home() {
        return "index";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        return "login";
    }
    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String userPage() {
        return "user-page";
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public String adminPage() {
        return "admin-page";
    }
    @RequestMapping("/403")
    public String forbidden() {
        return "403";
    }
}


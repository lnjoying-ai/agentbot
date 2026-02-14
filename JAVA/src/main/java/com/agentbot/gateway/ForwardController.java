package com.agentbot.gateway;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Forward Controller
 * Redirects frontend routes to index.html to allow Vue Router to handle them.
 */
@Controller
public class ForwardController {

    @RequestMapping(value = {"/chat", "/monitor", "/config"})
    public String forward() {
        return "forward:/";
    }
}

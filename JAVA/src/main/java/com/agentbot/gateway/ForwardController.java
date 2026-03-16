package com.agentbot.gateway;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA Forward Controller
 * Redirects frontend routes to index.html to allow Vue Router to handle them.
 */
@Controller
public class ForwardController {

    @RequestMapping(value = {
            "/",
            "/chat",
            "/multi-agent",
            "/p2p-chat",
            "/agents",
            "/skills",
            "/skills/store",
            "/cron",
            "/monitor",
            "/config"
    })
    public String forward() {
        return "forward:/index.html";
    }

}



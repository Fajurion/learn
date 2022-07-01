package com.fajurion.learn.frontend.main;

import com.fajurion.learn.repository.account.session.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
public class MainPageController {

    @Autowired
    private SessionRepository sessionRepository;

    @RequestMapping("/main")
    public Mono<String> mainTest() {
        return sessionRepository.count().map(c -> "reactive");
    }

}

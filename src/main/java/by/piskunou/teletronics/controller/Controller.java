package by.piskunou.teletronics.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class Controller {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello Teletronics!";
    }
}

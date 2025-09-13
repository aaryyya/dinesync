package com.dinesync.dinesync.UserController;

import com.dinesync.dinesync.UserEntity.User;
import com.dinesync.dinesync.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    UserService userService;
    @GetMapping("/")
    public String hello(){
        return "hello World";
    }
    @GetMapping("/user") //get request
    public ResponseEntity<List<User>> getAllUsers(){
        return ResponseEntity.ok(userService.getAll());
    }
}

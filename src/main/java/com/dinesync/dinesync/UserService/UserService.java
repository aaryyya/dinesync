package com.dinesync.dinesync.UserService;

import com.dinesync.dinesync.UserEntity.User;
import com.dinesync.dinesync.UserRepository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    UserRepository userRepository;
    public List<User> getAll(){
        return userRepository.findAll();
    }
}

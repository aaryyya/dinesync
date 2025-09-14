package com.dinesync.dinesync.UserService;

import com.dinesync.dinesync.UserEntity.User;
import com.dinesync.dinesync.UserRepository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

//service uses jpa to fetch data from database
//repository is a connection
//find all is select(selects all columns from users) selecting columns name wise is less expensive than doing select*
//service is used by controller
@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    public List<User> getAll(){
        return userRepository.findAll();
    }
    public User addUser(User user) { return userRepository.save(user); }

    public Optional<User> deleteUser(Long id){
       Optional<User> delUser=userRepository.findById(id);
        delUser.ifPresent(user -> userRepository.delete(user));
       return delUser;

    }

    public User getUserbyId(Long id){
        Optional<User> userById=userRepository.findById(id);
        if(userById.isPresent()){
            return userById.get();
        }
        return null;
    }

//    public User updateUser(User newUser,Long id){
//        Optional<User> userById=userRepository.findById(id);
//        if(userById.isPresent()){
//            userRepository.save(newUser);
//            return userById.get();
//        }
//        return null;
//
//    }
    public User updateUser(Long id, User user) {
        Optional<User> updateUser = userRepository.findById(id);
        if(updateUser.isPresent()) {
            User newUser = updateUser.get();
            if(user.getUserName() != null) {
                newUser.setUserName(user.getUserName());
            }
            if(user.getEmail() != null) {
                newUser.setEmail(user.getEmail());
            }
            userRepository.save(newUser);
            return newUser;
        }
        return null;
    }
}

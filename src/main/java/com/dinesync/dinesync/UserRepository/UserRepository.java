package com.dinesync.dinesync.UserRepository;

import com.dinesync.dinesync.UserEntity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
//repository acts as a connection between ur database and application
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
//    public findAll()
}

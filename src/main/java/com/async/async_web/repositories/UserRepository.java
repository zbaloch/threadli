package com.async.async_web.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.async.async_web.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {  
    // @Query("SELECT u FROM User u WHERE u.email = ?1")
    public User findByEmail(String email);
    public User findByEmailAndToken(String email, String token);
} 
    

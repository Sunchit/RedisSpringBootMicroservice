package com.codingdecoded.redis.redis.service;

import com.codingdecoded.redis.redis.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;
interface UserRepository extends JpaRepository<User, Long> {}

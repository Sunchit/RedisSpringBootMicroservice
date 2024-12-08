package com.codingdecoded.redis.redis.service;

import com.codingdecoded.redis.redis.dto.User;
import com.codingdecoded.redis.redis.service.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper instance

    public User createUser(User newUser) throws Exception {
        User savedUser = userRepository.save(newUser);
        String userForCache=objectMapper.writeValueAsString(savedUser);
        // Cache the newly created user in Redis
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        valueOps.set("user:" + savedUser.getId(), userForCache, 100, TimeUnit.SECONDS);
        return savedUser;
    }

    public ResponseEntity<User> getUser(Long id) {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        String cachedUser = valueOps.get("user:" + id);

        if (cachedUser != null) {
            try {
                // Deserialize the cached JSON string to a User object
                User user = objectMapper.readValue(cachedUser, User.class);
                return ResponseEntity.ok(user);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(500).build(); // Internal Server Error
            }
        } else {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                try {
                    // Serialize the User object to a JSON string and cache it
                    String userJson = objectMapper.writeValueAsString(user);
                    valueOps.set("user:" + id, userJson);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(500).build(); // Internal Server Error
                }
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
        }
    }

    public ResponseEntity<String> updateUser(Long id, User updatedUser) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            userRepository.save(existingUser);

            ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
            valueOps.set("user:" + existingUser.getId(), existingUser.toString(), 100, TimeUnit.SECONDS);

            return ResponseEntity.ok("User updated in H2 Database and cached in Redis");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<String> deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            redisTemplate.delete("user:" + id);
            return ResponseEntity.ok("User deleted from H2 Database and Redis cache");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
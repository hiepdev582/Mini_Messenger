package com.hiepnn.mini_messeger.service;

import com.hiepnn.mini_messeger.model.Friendship;
import com.hiepnn.mini_messeger.model.User;
import com.hiepnn.mini_messeger.repository.FriendshipRepository;
import com.hiepnn.mini_messeger.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final PresenceService presenceService;

    public UserService(UserRepository userRepository,
                       FriendshipRepository friendshipRepository,
                       PresenceService presenceService) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.presenceService = presenceService;
    }

    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isEmpty() || !existingUser.get().getPassword().equals(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return existingUser.get();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("Cannot add yourself as a friend");
        }
        if (friendshipRepository.existsByUserIdAndFriendId(userId, friendId)) {
            throw new IllegalArgumentException("Already friends");
        }
        Friendship friendship1 = Friendship.builder().userId(userId).friendId(friendId).build();
        Friendship friendship2 = Friendship.builder().userId(friendId).friendId(userId).build();
        friendshipRepository.saveAll(Arrays.asList(friendship1, friendship2));
    }

    public List<Map<String, Object>> getFriendsWithPresence(Long userId) {
        List<Friendship> friendships = friendshipRepository.findByUserId(userId);
        List<Map<String, Object>> friendsList = new ArrayList<>();
        for (Friendship friendship : friendships) {
            userRepository.findById(friendship.getFriendId()).ifPresent(user -> {
                Map<String, Object> friendMap = new HashMap<>();
                friendMap.put("id", user.getId());
                friendMap.put("username", user.getUsername());
                friendMap.put("displayName", user.getDisplayName());
                friendMap.put("status", presenceService.getUserPresence(user.getId()));
                friendsList.add(friendMap);
            });
        }
        return friendsList;
    }
}

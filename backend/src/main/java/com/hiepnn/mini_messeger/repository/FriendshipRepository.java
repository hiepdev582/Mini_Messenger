package com.hiepnn.mini_messeger.repository;

import com.hiepnn.mini_messeger.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    List<Friendship> findByUserId(Long userId);
    
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
}

package com.girlfriend.bot.repository;

import com.girlfriend.bot.model.entity.UserRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RelationshipRepository extends JpaRepository<UserRelationship, Long> {
    Optional<UserRelationship> findByChatUser(String chatUser);
}
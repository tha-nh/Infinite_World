package com.infinite.user.repository;
import com.infinite.user.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
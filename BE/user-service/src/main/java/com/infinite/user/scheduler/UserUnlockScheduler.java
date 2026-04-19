package com.infinite.user.scheduler;

import com.infinite.user.model.User;
import com.infinite.user.repository.UserRepository;
import com.infinite.user.util.Contant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserUnlockScheduler {
    
    private final UserRepository userRepository;
    
    @Scheduled(cron = "0 0 0 * * ?") // Chạy lúc 0h sáng hàng ngày
    @Transactional
    public void unlockExpiredUsers() {
        log.info("Starting scheduled unlock job at {}", LocalDateTime.now());
        
        try {
            List<User> lockedUsers = userRepository.findLockedUsersToUnlock(LocalDateTime.now(), Contant.IS_ACTIVE.LOCKED);
            
            int unlockedCount = 0;
            for (User user : lockedUsers) {
                user.setActive(Contant.IS_ACTIVE.ACTIVE);
                user.setLockTime(null);
                user.setModifiedBy("SYSTEM");
                userRepository.save(user);
                unlockedCount++;
                
                log.info("Unlocked user: {} (ID: {})", user.getUsername(), user.getId());
            }
            
            log.info("Scheduled unlock job completed. Unlocked {} users", unlockedCount);
            
        } catch (Exception e) {
            log.error("Error in scheduled unlock job: ", e);
        }
    }
}
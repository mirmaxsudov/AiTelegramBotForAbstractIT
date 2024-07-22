package uz.abdurahmon.aitelegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.abdurahmon.aitelegrambot.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);

    @Query("SELECT u FROM User u WHERE u.userRole = 'ADMIN'")
    List<User> getAdmins();
}

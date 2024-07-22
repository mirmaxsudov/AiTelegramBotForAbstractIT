package uz.abdurahmon.aitelegrambot.service.base;

import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;

import java.util.List;

public interface UserService {
    User getByChatId(Long chatId);
    void login(LoginDto loginDto);

    void save(User user);

    void deleteById(Long userId);

    String aboutMe(User user);

    List<User> getAdmins();
}

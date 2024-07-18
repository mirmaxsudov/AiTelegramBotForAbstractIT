package uz.abdurahmon.aitelegrambot.service.base;

import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;

public interface UserService {
    User getByChatId(Long chatId);
    void login(LoginDto loginDto);
}

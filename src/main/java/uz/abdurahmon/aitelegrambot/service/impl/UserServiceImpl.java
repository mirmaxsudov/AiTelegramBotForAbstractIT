package uz.abdurahmon.aitelegrambot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;
import uz.abdurahmon.aitelegrambot.repository.UserRepository;
import uz.abdurahmon.aitelegrambot.service.base.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public User getByChatId(Long chatId) {
        return userRepository.findByChatId(chatId).orElse(null);
    }

    @Override
    public void login(LoginDto loginDto) {
        User user = new User();
        user.setUserRole(loginDto.getUserRole());
        user.setChatId(loginDto.getChatId());
        user.setFirstName(loginDto.getFirstName());
        user.setLastName(loginDto.getLastName());
        user.setLanguage(loginDto.getLanguage());
        user.setPhoneNumber(loginDto.getPhoneNumber());
        userRepository.save(user);
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteById(Long userId) {
        userRepository.deleteById(userId);
    }
}
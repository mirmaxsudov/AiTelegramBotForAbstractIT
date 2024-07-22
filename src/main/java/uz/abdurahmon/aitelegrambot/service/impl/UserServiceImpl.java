package uz.abdurahmon.aitelegrambot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;
import uz.abdurahmon.aitelegrambot.repository.UserRepository;
import uz.abdurahmon.aitelegrambot.service.base.UserService;

import java.util.List;

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

        User user = getByChatId(loginDto.getChatId());

        if (user == null) {
            User newUser = new User();
            newUser.setUserRole(loginDto.getUserRole());
            newUser.setChatId(loginDto.getChatId());
            newUser.setFirstName(loginDto.getFirstName());
            newUser.setLastName(loginDto.getLastName());
            newUser.setLanguage(loginDto.getLanguage());
            newUser.setPhoneNumber(loginDto.getPhoneNumber());
            newUser.setDeleted(false);
            userRepository.save(newUser);
        } else {
            user.setDeleted(false);
            userRepository.save(user);
        }
    }

    @Override
    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void deleteById(Long userId) {
        User user = getByUserId(userId);
        if (user == null)
            return;

        user.setDeleted(true);
        save(user);
    }

    private User getByUserId(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public String aboutMe(User user) {

        String base = """
                ```%s
                %s - %s
                %s - %s
                %s - %s
                %s - %s
                %s - +%s
                %s - %s
                %s - %s```""";

        Language language = user.getLanguage();
        if (language.equals(Language.ENGLISH)) {
            return String.format(base,
                    "About_me",
                    "Id", user.getId(),
                    "First name", user.getFirstName(),
                    "Last name", user.getLastName() == null ? "N/A" : user.getLastName(),
                    "Phone number", user.getPhoneNumber(),
                    "Language", user.getLanguage(),
                    "Chat id", user.getChatId(),
                    "User role", user.getUserRole());
        } else if (language.equals(Language.UZBEK)) {
            return String.format(base,
                    "Men-haqimda",
                    "Id", user.getId(),
                    "Ismi", user.getFirstName(),
                    "Familiyasi", user.getLastName() == null ? "N/A" : user.getLastName(),
                    "Telefon raqami", user.getPhoneNumber(),
                    "Til", user.getLanguage(),
                    "Chat id", user.getChatId(),
                    "Role", user.getUserRole());
        } else {
            return String.format(base,
                    "Обо-мне",
                    "ИД", user.getId(),
                    "Имя", user.getFirstName(),
                    "Фамилия", user.getLastName() == null ? "N/A" : user.getLastName(),
                    "Номер телефона", user.getPhoneNumber(),
                    "Язык", user.getLanguage(),
                    "ИД чата", user.getChatId(),
                    "Роль", user.getUserRole());
        }
    }

    @Override
    public List<User> getAdmins() {
        return userRepository.getAdmins();
    }
}
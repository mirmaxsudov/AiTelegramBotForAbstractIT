package uz.abdurahmon.aitelegrambot.service.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.abdurahmon.aitelegrambot.config.BotConfiguration;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;
import uz.abdurahmon.aitelegrambot.entity.enums.UserRole;
import uz.abdurahmon.aitelegrambot.service.base.UserService;
import uz.abdurahmon.aitelegrambot.service.bot.enums.Operation;
import uz.abdurahmon.aitelegrambot.service.bot.replyMarkups.ReplyMarkup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@SuppressWarnings("all")
public class TelegramBot extends TelegramLongPollingBot implements ReplyMarkup {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfiguration botConfiguration;
    private final UserService userService;

    @Autowired
    public TelegramBot(UserService userService, BotConfiguration botConfiguration) {
        this.userService = userService;
        this.botConfiguration = botConfiguration;

        List<BotCommand> commands = List.of(
                new BotCommand("/start", "Start the bot🔰"),
                new BotCommand("/info", "Get info regarding Bot🤖"),
                new BotCommand("/help", "Find help🆘"));

        try {
            execute(new SetMyCommands(commands, new BotCommandScopeChat(), null));
        } catch (Exception e) {
        }
    }


    @Override
    public String getBotUsername() {
        return botConfiguration.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfiguration.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> {
            if (update.hasMessage()) {
                proccessUpdate(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                proccessCallbackQuery(update.getCallbackQuery());
            }
        });
    }

    private final static Map<Long, Operation> MP = new HashMap<>();
    private final static Map<Long, LoginDto> LOGIN_DTO_MAP = new HashMap<>();

    private void proccessCallbackQuery(CallbackQuery callbackQuery) {
    }

    private void proccessUpdate(Message message) {
        final Long chatId = message.getChatId();
        final String text = message.getText();
        final int messageId = message.getMessageId();
        final User user = userService.getByChatId(chatId);
        final Operation operation = MP.get(chatId);

        if (user == null) {
            if (operation == null) {
                login(chatId, messageId);
            } else {
                switch (operation) {
                    case LOGIN_PHONE_NUMBER -> {
                        login(chatId, messageId, message.getContact());
                    }
                    case LOGIN_LANGUAGE -> {
                        login(chatId, messageId, text);
                    }
                }
            }
            return;
        }

        final UserRole role = user.getUserRole();
        boolean isUsed = false;

        if (text.equals("/start")) {
            switch (role) {
                case USER -> showUserMenu(chatId, user.getLanguage());
            }

            return;
        }

        if (role.equals(UserRole.USER)) isUsed = forUserMenu(chatId, messageId, text, user);

        if (isUsed) return;

        switch (operation) {
            case TEXT_TO_SPEECH -> textToSpeech(chatId, messageId, user, text);
        }
    }

    private void textToSpeech(Long chatId, int messageId, User user, String text) {
        Language language = user.getLanguage();
        sendMessage(chatId, language.equals(Language.ENGLISH) ? "Processing... ⏳" : language.equals(Language.UZBEK) ? "Ishlanmoqda ... ⏳" : language.equals(Language.RUSSIAN) ? "Обработка... ⏳" : "Qayta ishlanmoqda ... ⏳");

        SendAudio audio = new SendAudio();
        audio.setChatId(chatId);
        audio.setReplyToMessageId(messageId);
        audio.setAudio(new InputFile(
                new File(
                        "src/main/resources/files/audio.mp3"
                )
        ));
        audio.setPerformer("Abdurahmon");
        audio.setTitle("Text to speech");
        audio.setCaption(language.equals(Language.ENGLISH) ? "Done ✅" : language.equals(Language.UZBEK) ? "Yakunlandi ✅" : "Готово ✅");

        try {
            execute(audio);
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        } finally {
            MP.remove(chatId);
        }
    }

    private void showUserMenu(Long chatId, Language language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(language.equals(Language.ENGLISH) ? "Choose" : language.equals(Language.UZBEK) ? "Tanlang" : "Выбирать");
        sendMessage.setReplyMarkup(getReplyKeyboardMainMenuForUser(language));
        executeCustom(sendMessage);
    }

    private boolean forUserMenu(Long chatId, Integer messageId, String text, User user) {
        switch (text) {
            case "Text to speech 🔊" -> textToSpeech(chatId, messageId, user);
            case "Biz haqimizda ℹ️", "About us ℹ️", "О нас ℹ️" -> aboutUs(chatId, messageId, user);
            case "Sozlamalar ⚙️", "Settings ⚙️", "Настройки ⚙️" -> settings(chatId, user);
            case "Orqaga ⬅️", "Back ⬅️", "Назад ⬅️" -> showUserMenu(chatId, user.getLanguage());
            case "Change language 🇺🇸🇺🇿🇷🇺", "Tilni o'zgartirish 🇺🇿🇷🇺🇺🇸", "Изменить язык 🇷🇺🇺🇸🇺🇿" ->
                    changeLanguage(chatId, messageId, user);
            case "Cache tozalash ♻️", "Clear cache ♻️", "Очистить кэш ♻️" -> clearCache(chatId, user);
        }
        return false;
    }

    private void textToSpeech(Long chatId, Integer messageId, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyToMessageId(messageId);
        message.setText(user.getLanguage().equals(Language.ENGLISH) ?
                "Enter text" : user.getLanguage().equals(Language.UZBEK) ?
                "Matn kiriting" : "Введи текст");

        MP.put(chatId, Operation.TEXT_TO_SPEECH);
        executeCustom(message);
    }

    private void clearCache(Long chatId, User user) {
        Language language = user.getLanguage();

        sendMessage(chatId, language.equals(Language.ENGLISH) ? "Your datas are being clearing🧹" : language.equals(Language.UZBEK) ? "Siz ma'lumotlarni tozalanmoqda🧹" : "Ваши данные очищаются🧹");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyMarkup(deleteReplyMarkup());
        message.setText(language.equals(Language.ENGLISH) ? "Cleared🧹" : language.equals(Language.UZBEK) ? "Tozalandi🧹" : "Очищено🧹");
        executeCustom(message);

        userService.deleteById(user.getId());
    }

    private void changeLanguage(Long chatId, Integer messageId, User user) {
        user.setLanguage(user.getLanguage().equals(Language.ENGLISH) ? Language.UZBEK : user.getLanguage().equals(Language.UZBEK) ? Language.RUSSIAN : Language.ENGLISH);
        userService.save(user);
        settings(chatId, user);
    }

    private void settings(Long chatId, User user) {
        SendMessage mainSettings = new SendMessage();
        mainSettings.setChatId(chatId);
        mainSettings.setText(user.getLanguage().equals(Language.ENGLISH) ? "Settings choose" :
                user.getLanguage().equals(Language.UZBEK) ?
                        "Sozlamalar oynasiga xush kelibsiz" :
                        "Настройки выбираются");
        mainSettings.setReplyMarkup(getReplyKeyboardForSettings(user.getLanguage()));
        executeCustom(mainSettings);
    }

    private void aboutUs(Long chatId, Integer messageId, User user) {
        String aboutUs = getAboutUsByLanguage(user.getLanguage());
        sendMessage(chatId, aboutUs);
    }

    private String getAboutUsByLanguage(Language language) {
        String filePath = switch (language) {
            case UZBEK -> "src/main/resources/aboutUsUz.txt";
            case RUSSIAN -> "src/main/resources/aboutUsRu.txt";
            default -> "src/main/resources/aboutUsEng.txt";
        };

        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String currentLine;

            while ((currentLine = reader.readLine()) != null)
                stringBuilder.append(currentLine).append("\n");
        } catch (
                IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return stringBuilder.toString();
    }

    private void login(Long chatId, int messageId, String language) {
        if (!checkLan(language)) {
            sendMessage(chatId, "Choose correct language");
            return;
        }

        LoginDto loginDto = LOGIN_DTO_MAP.get(chatId);
        loginDto.setLanguage(Language.getEnumByName(language));

        userService.login(loginDto);

        MP.remove(chatId);
        LOGIN_DTO_MAP.remove(chatId);

        SendMessage success = new SendMessage();
        success.setChatId(chatId);
        success.setText("Choose");
        success.setReplyMarkup(deleteReplyMarkup());

        executeCustom(success);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        executeCustom(sendMessage);
    }

    private boolean checkLan(String language) {
        return switch (language) {
            case "Uzbek 🇺🇿", "English 🇺🇸", "Russian 🇷🇺" -> true;
            default -> false;
        };
    }

    private void login(Long chatId, Integer messageId, Contact contact) {
        LoginDto loginDto = LOGIN_DTO_MAP.get(chatId);

        loginDto.setLastName(contact.getLastName());
        loginDto.setFirstName(contact.getFirstName());
        loginDto.setPhoneNumber(contact.getPhoneNumber());

        MP.put(chatId, Operation.LOGIN_LANGUAGE);

        SendMessage message = new SendMessage();
        message.setText("Enter your language");
        message.setReplyMarkup(replyForLoginLanguage());
        message.setReplyToMessageId(messageId);
        message.setChatId(chatId);

        executeCustom(message);
    }

    private void login(Long chatId, int messageId) {
        LoginDto loginDto = LoginDto.builder()
                .userRole(UserRole.USER)
                .chatId(chatId)
                .build();

        MP.put(chatId, Operation.LOGIN_PHONE_NUMBER);
        LOGIN_DTO_MAP.put(chatId, loginDto);

        SendMessage login = new SendMessage();
        login.setText("Enter your phone number");
        login.setReplyMarkup(replyForLoginPhoneNumber());
        login.setReplyToMessageId(messageId);
        login.setChatId(chatId);

        executeCustom(login);
    }

    private void executeCustom(SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            log.error("Error occurred: ", e);
        }
    }
}
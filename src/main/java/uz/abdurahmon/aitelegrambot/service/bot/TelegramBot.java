package uz.abdurahmon.aitelegrambot.service.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import uz.abdurahmon.aitelegrambot.config.BotConfiguration;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;
import uz.abdurahmon.aitelegrambot.entity.enums.UserRole;
import uz.abdurahmon.aitelegrambot.service.base.UserService;
import uz.abdurahmon.aitelegrambot.service.bot.enums.Operation;
import uz.abdurahmon.aitelegrambot.service.bot.replyMarkups.ReplyMarkup;

import java.io.BufferedReader;
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

        }
    }

    private void showUserMenu(Long chatId, Language language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(language.equals(Language.ENGLISH) ? "Choose" : "Tanlang");
        sendMessage.setReplyMarkup(getReplyKeyboardMainMenuForUser(language));
        executeCustom(sendMessage);
    }

    private boolean forUserMenu(Long chatId, Integer messageId, String text, User user) {
        switch (text) {
            case "Biz haqimizda ℹ️", "About us ℹ️" -> aboutUs(chatId, messageId, user);
        }
        return false;
    }

    private void aboutUs(Long chatId, Integer messageId, User user) {
        String aboutUs = getAboutUsByLanguage(user.getLanguage());
        sendMessage(chatId, aboutUs);
    }

    private String getAboutUsByLanguage(Language language) {
        String filePath;
        if (language.equals(Language.ENGLISH)) {
            filePath = "src/main/resources/aboutUsEng.txt";
        } else {
            filePath = "src/main/resources/aboutUsUz.txt";
        }

        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(filePath));
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                stringBuilder.append(currentLine).append("\n");
            }
        } catch (IOException e) {
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
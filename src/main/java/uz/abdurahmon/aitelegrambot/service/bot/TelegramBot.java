package uz.abdurahmon.aitelegrambot.service.bot;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeChat;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.abdurahmon.aitelegrambot.config.BotConfiguration;
import uz.abdurahmon.aitelegrambot.entity.Attachment;
import uz.abdurahmon.aitelegrambot.entity.Feedback;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.dto.LoginDto;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;
import uz.abdurahmon.aitelegrambot.entity.enums.UserRole;
import uz.abdurahmon.aitelegrambot.service.base.AttachmentService;
import uz.abdurahmon.aitelegrambot.service.base.DownloadImgService;
import uz.abdurahmon.aitelegrambot.service.base.FeedbackService;
import uz.abdurahmon.aitelegrambot.service.base.UserService;
import uz.abdurahmon.aitelegrambot.service.bot.enums.Operation;
import uz.abdurahmon.aitelegrambot.service.bot.inlineKeyBoards.InlineMarkup;
import uz.abdurahmon.aitelegrambot.service.bot.replyMarkups.ReplyMarkup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@SuppressWarnings("all")
public class TelegramBot extends TelegramLongPollingBot implements ReplyMarkup, InlineMarkup {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final BotConfiguration botConfiguration;
    private final UserService userService;
    private final FeedbackService feedbackService;
    private final AttachmentService attachmentService;
    private final DownloadImgService downloadImgService;

    @Autowired
    public TelegramBot(UserService userService, BotConfiguration botConfiguration, FeedbackService feedbackService, AttachmentService attachmentService, DownloadImgService downloadImgService) {
        this.userService = userService;
        this.botConfiguration = botConfiguration;
        this.feedbackService = feedbackService;
        this.attachmentService = attachmentService;
        this.downloadImgService = downloadImgService;

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
        try {
            if (update.hasMessage()) {
                proccessUpdate(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                proccessCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private final static Map<Long, Operation> MP = new HashMap<>();
    private final static Map<Long, LoginDto> LOGIN_DTO_MAP = new HashMap<>();

    private void proccessCallbackQuery(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage message = callbackQuery.getMessage();

        final Long chatId = message.getChatId();
        final String data = callbackQuery.getData();
        final int messageId = message.getMessageId();

        System.out.println("data = " + data);

        User user = userService.getByChatId(chatId);

        if (user == null) {
            AnswerCallbackQuery login = new AnswerCallbackQuery();
            login.setText("Avval tizimga kirishingiz kerak 🔐. Kirish uchun /start buyrug'idan foydalaning");
            login.setShowAlert(true);
            login.setCallbackQueryId(callbackQuery.getId());
            executeCustom(login);
            return;
        }

        if (data == null) return;

        if (data.startsWith("SEND_FEEDBACK: ")) {
            sendFeedbackToAdmins(data, chatId, messageId, user, callbackQuery.getId());
        } else if (data.startsWith("DELETE_FEEDBACK: ")) {
            deleteFeedback(data, user, messageId, callbackQuery.getId());
        } else if (data.startsWith("SEND_ATTACHMENT_TO_ANALYZE: ")) {
            sendAttachmentToAnalyze(data, user, messageId);
        } else if (data.startsWith("DELETE_ATTACHMENT: ")) {
            System.out.println("messageId = " + messageId);
            System.out.println("chatId = " + chatId);
            deleteAttachment(data, messageId, user);
        }
    }

    private void deleteAttachment(String data, int messageId, User user) {
        Long attachmentId = Long.valueOf(data.split(" ")[1]);
        attachmentService.deleteById(attachmentId);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(
                user.getLanguage().equals(Language.UZBEK) ? "O'chirib yuborildi" :
                        user.getLanguage().equals(Language.ENGLISH) ?
                                "Deleted successfully" : "Удален успешно"
        );
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(messageId);
        executeCustom(editMessageText);
    }

    private void sendAttachmentToAnalyze(String data, User user, int messageId) {
        Long attachmentId;
        try {
            attachmentId = Long.parseLong(data.split(" ")[1]);
        } catch (Exception e) {
            log.error("Error occurred: {}", e.getMessage());
            return;
        }

        Attachment attachment = attachmentService.getById(attachmentId);

        if (attachment == null)
            return;

        sendMessage(user.getChatId(), "Done");
    }

    private void deleteFeedback(String data, User user, int messageId, String callbackId) {
        Feedback feedback;
        try {
            long feedbackId = Long.parseLong(data.split(" ")[1]);
            feedback = feedbackService.getById(feedbackId);

            if (feedback == null)
                throw new Exception("Feedback not found");
        } catch (Exception e) {
            AnswerCallbackQuery notFound = new AnswerCallbackQuery();
            notFound.setText(user.getLanguage().equals(Language.ENGLISH) ? "Feedback not found 🤷‍♂️" :
                    user.getLanguage().equals(Language.UZBEK) ? "Fikr topilmadi 🤷‍♂️" :
                            "Комментарий не найден 🤷‍♂️");
            notFound.setCallbackQueryId(callbackId);
            executeCustom(notFound);
            return;
        }

        feedbackService.deleteById(feedback.getId());

        EditMessageText edit = new EditMessageText();
        edit.setChatId(user.getChatId());
        edit.setMessageId(messageId);
        edit.setText(
                user.getLanguage().equals(Language.ENGLISH) ?
                        "Your feedback has been deleted successfully ✅" :
                        user.getLanguage().equals(Language.UZBEK) ?
                                "Fikr muvaffaqiyatli o'chirildi ✅" :
                                "Ваш комментарий был успешно удален ✅"
        );
        executeCustom(edit);
    }

    private void sendFeedbackToAdmins(String data, Long chatId, int messageId, User user, String callbackId) {
        Long feedbackId = Long.parseLong(data.split(" ")[1]);
        System.out.println("feedbackId = " + feedbackId);
        Feedback feedback = feedbackService.getById(feedbackId);
        Language language = user.getLanguage();

        if (feedback == null) {
            AnswerCallbackQuery notFound = new AnswerCallbackQuery();
            notFound.setText(language.equals(Language.ENGLISH) ?
                    "Feedback not found 🤷‍♂️" : language.equals(Language.UZBEK) ?
                    "Fikr topilmadi 🤷‍♂️" : "Комментарий не найден 🤷‍♂️");
            notFound.setCallbackQueryId(data);
            executeCustom(notFound);
            return;
        }

        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(messageId);
        edit.setText((language.equals(Language.ENGLISH) ?
                "Your feedback has been sent to admins: " : language.equals(Language.UZBEK) ?
                "Sizning fikringiz telegram admins bilan yuborildi: " :
                "Ваш отзыв отправлен администраторам:") + feedback.getText());
        executeCustom(edit);

        CompletableFuture.runAsync(() -> sendToAdmins(feedback, user));

        CompletableFuture.runAsync(() -> {
            SendMessage groupMessage = new SendMessage();
            groupMessage.setText("Yangi fikr qabul qilindi: " + feedback.getText() + "\n" +
                    String.format(
                            "Foydalanuvchi - %s. \nTelefon raqami - %s\nYuborilgan vaqti - %s",
                            user.getFirstName(), user.getPhoneNumber(), feedback.getSendTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    ));
            groupMessage.setChatId(botConfiguration.getGroupId());
            groupMessage.setReplyMarkup(getFeedbackForGroup(feedbackId));
            executeCustom(groupMessage);
        });
    }

    private void sendToAdmins(Feedback feedback, User user) {
        List<User> admins = userService.getAdmins();

        if (admins.isEmpty())
            return;

        for (User admin : admins) {
            Language language = admin.getLanguage();

            SendMessage message = new SendMessage();
            message.setText(language.equals(Language.ENGLISH) ?
                    "New feedback was received: " + feedback.getText() + "\n" +
                            String.format(
                                    "Name of sender - %s. \nPhone number - %s\nSent at - %s",
                                    user.getFirstName(), user.getPhoneNumber(), feedback.getSendTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            ) : language.equals(Language.UZBEK) ?
                    "Yangi fikr qabul qilindi: " + feedback.getText() + "\n" +
                            String.format(
                                    "Foydalanuvchi - %s. \nTelefon raqami - %s\nYuborilgan vaqti - %s",
                                    user.getFirstName(), user.getPhoneNumber(), feedback.getSendTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            ) :
                    "Новый отзыв получен: " + feedback.getText() + "\n" +
                            String.format(
                                    "Имя отправителя - %s. \nНомер телефона - %s\nОтправлено - %s",
                                    user.getFirstName(), user.getPhoneNumber(), feedback.getSendTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            ));
            message.setChatId(admin.getChatId());
            executeCustom(message);
        }
    }

    private void executeCustom(EditMessageText edit) {
        try {
            execute(edit);
        } catch (Exception e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private void executeCustom(AnswerCallbackQuery callbackQuery) {
        try {
            execute(callbackQuery);
        } catch (Exception e) {
            log.error("Error occurred: {}", e.getMessage());
        }
    }

    private void proccessUpdate(Message message) {
        final Long chatId = message.getChatId();

        if (chatId.toString().equals(botConfiguration.getGroupId()))
            return;

        final String text = message.getText();
        final int messageId = message.getMessageId();
        User user = userService.getByChatId(chatId);
        user = user.isDeleted() ? null : user;
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

        if (message.hasPhoto()) {
            handlePhoto(user, messageId, message.getPhoto());
            return;
        }

        if (text.equals("/start")) {
            switch (role) {
                case USER -> showUserMenu(chatId, user.getLanguage());
            }
            return;
        }

        if (role.equals(UserRole.USER)) isUsed = forUserMenu(chatId, messageId, text, user);

        if (isUsed || operation == null)
            return;

        switch (operation) {
            case TEXT_TO_SPEECH -> textToSpeech(chatId, messageId, user, text);
            case ASK_FEEDBACK_TEXT -> askFeedbackText(chatId, user, text);
            case TEXT_TO_TEXT_ASKED_TEXT -> textToTextAskedText(chatId, user, text);
            case IMAGE_TO_TEXT_ASKED_IMG_LINK -> imageToTextAskedLink(chatId, messageId, user, text);
        }
    }

    private void handlePhoto(User user, int messageId, List<PhotoSize> photo) {
        Operation operation = MP.get(user.getChatId());
        MP.remove(user.getChatId());

        if (photo.isEmpty() || operation == null)
            return;

        switch (operation) {
            case IMAGE_TO_SPEECH_ASKED_IMAGE -> {
                String url = downloadImgToLocalAndReturnURL(user, photo);
                imageToSpeech(user, messageId, url);
            }
            case IMAGE_TO_TEXT_INNER_ASKED_IMAGE -> {
                String url = downloadImgToLocalAndReturnURL(user, photo);
                imageToText(user, messageId, url);
            }
        }
    }

    private void imageToText(User user, int messageId, String url) {

    }

    private void imageToSpeech(User user, int messageId, String imgURL) {
        if (imgURL == null) {
            SendMessage message = new SendMessage();
            message.setChatId(user.getChatId());
            message.setText(user.getLanguage().equals(Language.ENGLISH) ? "Send image" :
                    user.getLanguage().equals(Language.UZBEK) ? "Rasm yuboring" : "Отправьте изображение");
            executeCustom(message);
            return;
        }

        SendPhoto photo = new SendPhoto();
        photo.setChatId(user.getChatId());
        photo.setPhoto(
                new InputFile(
                        new File(imgURL), "photo_" + user.getChatId() + "_" + Instant.now().getEpochSecond() + ".png"
                )
        );

        Attachment save = attachmentService.save(imgURL, user);

        photo.setCaption(user.getLanguage().equals(Language.ENGLISH) ? "Anayze image" :
                user.getLanguage().equals(Language.UZBEK) ?
                        "Rasmni analizatsiya qilish" : "Анализировать изображение");
        photo.setReplyMarkup(getInlineKeyboardMarkupToAskAnalyzeImage(save, user.getLanguage()));
        executeCustom(photo);

        MP.remove(user.getChatId());
    }

    private String downloadImgToLocalAndReturnURL(User user, List<PhotoSize> photo) {
        String fileId = photo.get(photo.size() - 1).getFileId();

        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);

            var file = execute(getFile);
            String filePath = file.getFilePath();

            String url = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);

            assert imageBytes != null;

            String baseURL = "src/main/resources/static/";

            if (!Files.exists(Paths.get(baseURL)))
                Files.createDirectory(Paths.get(baseURL));

            String fileName = baseURL + "photo_" + user.getChatId() + "_" + Instant.now().getEpochSecond() + ".png";
            Files.write(Paths.get(fileName), imageBytes);

            return fileName;
        } catch (TelegramApiException | IOException e) {
            log.error("Error occurred: {}", e.getMessage());
            return null;
        }
    }

    private void askFeedbackText(Long chatId, User user, String text) {
        MP.remove(chatId);

        Language language = user.getLanguage();

        Feedback feedback = Feedback.builder()
                .text(text)
                .user(user)
                .isRead(false)
                .isSend(false)
                .readTime(null)
                .sendTime(LocalDateTime.now())
                .build();

        feedbackService.save(feedback);

        SendMessage feedbackMessage = new SendMessage();
        feedbackMessage.setChatId(chatId);
        feedbackMessage.setText(language.equals(Language.ENGLISH) ? "Do you want to send feedback ?" :
                language.equals(Language.UZBEK) ? "Fikringizni yubormoqchimisiz ?" : "Вы хотите отправить отзыв ?");
        feedbackMessage.setReplyMarkup(getReplyKeyboardForFeedback(feedback.getId(), language));

        executeCustom(feedbackMessage);
    }

    private void textToTextAskedText(Long chatId, User user, String text) {
        MP.remove(chatId);
        sendMessage(chatId, user.getLanguage().equals(Language.ENGLISH) ? "Processing... ⏳" :
                user.getLanguage().equals(Language.UZBEK) ? "Yuklanmoqda ... ⏳" : "Обработка ... ⏳");

        sendMessage(chatId, "Done✅🤖");
    }

    private void imageToTextAskedLink(Long chatId, int messageId, User user, String text) {
        MP.remove(chatId);

        Language language = user.getLanguage();
        sendMessage(chatId, language.equals(Language.ENGLISH) ? "Processing... ⏳" :
                language.equals(Language.UZBEK) ? "Yuklanmoqda ... ⏳" : "Обработка ... ⏳");
        String download = downloadImgService.download(text);

        if (download == null) {
            sendMessage(
                    chatId, language.equals(Language.ENGLISH) ? "While processing, Error occurred ... ⏳" :
                            language.equals(Language.UZBEK) ? "Qayta ishlash jarayonida xatolik yuz berdi ... ⏳" : "Во время обработки произошла ошибка ... ⏳"
            );
            return;
        }

        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId);
        photo.setReplyToMessageId(messageId);
        photo.setPhoto(new InputFile(new File(download)));
        photo.setCaption(
                language.equals(Language.ENGLISH) ? "Do you want to analyze this image?" :
                        language.equals(Language.UZBEK) ? "Rasmni analiz qilmoqchimisiz?" :
                                "Вы хотите проанализировать эту картинку?"
        );
        photo.setReplyMarkup(getInlineKeyboardMarkupToAskAnalyzeImage(user));

        executeCustom(photo);
    }

    private void executeCustom(SendPhoto photo) {
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
        boolean isUsed = false;
        switch (text) {
            case "Send image \uD83D\uDCF7" -> {
                imageToTextInner(user);
                isUsed = true;
            }
            case "Image to speech 📝" -> {
                imageToSpeech(chatId, messageId, user);
                isUsed = true;
            }
            case "Image to text 📝" -> {
                imageToText(chatId, messageId, user);
                isUsed = true;
            }
            case "Text to text 📝" -> {
                textToText(chatId, messageId, user);
                isUsed = true;
            }
            case "Xabar berish 📝", "Feedback 📝", "Отправить сообщение 📝" -> {
                askFeedback(user);
                isUsed = true;
            }
            case "Linkni yuborish 📤", "Отправить ссылку 📤", "Send link 📤" -> {
                sendLinkForImageToText(chatId, messageId, user);
                isUsed = true;
            }
            case "Text to speech 🔊" -> {
                textToSpeech(chatId, messageId, user);
                isUsed = true;
            }
            case "Biz haqimizda ℹ️", "About us ℹ️", "О нас ℹ️" -> {
                aboutUs(chatId, messageId, user);
                isUsed = true;
            }
            case "Sozlamalar ⚙️", "Settings ⚙️", "Настройки ⚙️" -> {
                settings(chatId, user);
                isUsed = true;
            }
            case "Orqaga ⬅️", "Back ⬅️", "Назад ⬅️" -> {
                showUserMenu(chatId, user.getLanguage());
                isUsed = true;
            }
            case "Change language 🇺🇸🇺🇿🇷🇺", "Tilni o'zgartirish 🇺🇿🇷🇺🇺🇸", "Изменить язык 🇷🇺🇺🇸🇺🇿" -> {
                changeLanguage(chatId, messageId, user);
                isUsed = true;
            }
            case "Cache tozalash ♻️", "Clear cache ♻️", "Очистить кэш ♻️" -> {
                clearCache(chatId, user);
                isUsed = true;
            }
            case "Men haqimda ℹ️", "О себе ℹ️", "About me ℹ️" -> {
                aboutMe(user);
                isUsed = true;
            }
        }
        return isUsed;
    }

    private void imageToTextInner(User user) {
        Language language = user.getLanguage();

        sendMessage(user.getChatId(),
                language.equals(Language.ENGLISH) ? "Send image" :
                        language.equals(Language.UZBEK) ? "Rasm yuboring" : "Введи изображение");
        MP.put(user.getChatId(), Operation.IMAGE_TO_TEXT_INNER_ASKED_IMAGE);
    }

    private void imageToSpeech(Long chatId, Integer messageId, User user) {
        if (user == null) {
            sendMessage(chatId, """
                    Iltimos, /start buyrug'i bilan boshlash
                    Пожалуйста, /start, чтобы начать
                    Please, /start to start
                    """);
            return;
        }

        Language language = user.getLanguage();
        sendMessage(chatId, language.equals(Language.UZBEK) ?
                "Rasm yuboring" : language.equals(Language.ENGLISH) ?
                "Send image" : "Введи изображение");

        MP.put(chatId, Operation.IMAGE_TO_SPEECH_ASKED_IMAGE);
    }

    private void askFeedback(User user) {
        Language language = user.getLanguage();
        sendMessage(user.getChatId(), language.equals(Language.UZBEK) ?
                "Xabar yozing" : language.equals(Language.ENGLISH) ?
                "Enter your message" : "Напишите ваше сообщение");
        MP.put(user.getChatId(), Operation.ASK_FEEDBACK_TEXT);
    }

    private void textToText(Long chatId, int messageId, User user) {
        if (user == null) {
            sendMessage(chatId, """
                    Iltimos, /start buyrug'i bilan boshlash
                    Пожалуйста, /start, чтобы начать
                    Please, /start to start
                    """);
            return;
        }

        Language language = user.getLanguage();

        sendMessage(chatId, language.equals(Language.UZBEK) ?
                "Matnni yuboring" : language.equals(Language.ENGLISH) ?
                "Enter text" : "Введи текст");
        MP.put(chatId, Operation.TEXT_TO_TEXT_ASKED_TEXT);
    }

    private void aboutMe(User user) {
        SendMessage me = new SendMessage();
        me.setChatId(user.getChatId());
        me.setParseMode(ParseMode.MARKDOWNV2);
        me.setText(userService.aboutMe(user));

        executeCustom(me);
    }

    private void sendLinkForImageToText(Long chatId, Integer messageId, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyToMessageId(messageId);
        message.setText(user.getLanguage().equals(Language.ENGLISH) ?
                "Enter link" : user.getLanguage().equals(Language.UZBEK) ?
                "Linkni kiriting" : "Введи ссылку");

        MP.put(chatId, Operation.IMAGE_TO_TEXT_ASKED_IMG_LINK);
        executeCustom(message);
    }

    private void imageToText(Long chatId, int messageId, User user) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyToMessageId(messageId);
        message.setReplyMarkup(getReplyKeyboardForImageToText(user.getLanguage()));
        message.setText(user.getLanguage().equals(Language.ENGLISH) ?
                "Choose option" : user.getLanguage().equals(Language.UZBEK) ?
                "Tanlang" : "Выберите опцию");

        executeCustom(message);
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

    @SneakyThrows
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

        showUserMenu(chatId, loginDto.getLanguage());
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
        message.setText("""
                O'zingizga mos keladigan tillarni tanlang
                Choose your suitable language
                Выберите подходящий язык
                """);
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
package uz.abdurahmon.aitelegrambot.service.bot.replyMarkups;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;

import java.util.List;

public interface ReplyMarkup {
    default ReplyKeyboardMarkup replyForLoginPhoneNumber() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);
        markup.setIsPersistent(true);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);

        KeyboardButton phone = new KeyboardButton();
        phone.setRequestContact(true);
        phone.setText("Enter your phone number");

        KeyboardRow rw1 = new KeyboardRow();
        rw1.add(phone);

        markup.setKeyboard(List.of(rw1));

        return markup;
    }

    default ReplyKeyboardMarkup replyForLoginLanguage() {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(true);
        markup.setIsPersistent(true);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(true);

        KeyboardRow rw1 = new KeyboardRow();
        rw1.add("English 🇺🇸");
        rw1.add("Uzbek 🇺🇿");
        rw1.add("Russian 🇷🇺");

        markup.setKeyboard(List.of(rw1));

        return markup;
    }

    default ReplyKeyboard deleteReplyMarkup() {
        ReplyKeyboardRemove remove = new ReplyKeyboardRemove();
        remove.setSelective(true);
        remove.setRemoveKeyboard(true);
        return remove;
    }

    default ReplyKeyboardMarkup getReplyKeyboardMainMenuForUser(Language language) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(false);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        KeyboardRow rw1 = new KeyboardRow();
        KeyboardRow rw2 = new KeyboardRow();
        KeyboardRow rw3 = new KeyboardRow();
        KeyboardRow rw4 = new KeyboardRow();

        rw1.add("Image to text 📝");
        rw1.add("Image to speech 📝");
        rw2.add("Text to speech 🔊");
        rw2.add("Text to text 📝");

        if (language.equals(Language.ENGLISH)) {
            rw3.add("Feedback 📝");
            rw4.add("About us ℹ️");
            rw4.add("Settings ⚙️");
        } else if (language.equals(Language.UZBEK)) {
            rw3.add("Xabar berish 📝");
            rw4.add("Biz haqimizda ℹ️");
            rw4.add("Sozlamalar ⚙️");
        } else {
            rw3.add("Отправить сообщение 📝");
            rw4.add("О нас ℹ️");
            rw4.add("Настройки ⚙️");
        }

        markup.setKeyboard(List.of(rw1, rw2, rw3, rw4));

        return markup;
    }

    default ReplyKeyboardMarkup getReplyKeyboardForSettings(Language language) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setSelective(false);
        markup.setResizeKeyboard(true);
        markup.setOneTimeKeyboard(false);

        KeyboardRow rw1 = new KeyboardRow();
        KeyboardRow rw2 = new KeyboardRow();
        KeyboardRow rw3 = new KeyboardRow();

        if (language.equals(Language.ENGLISH)) {
            rw1.add("Change language 🇺🇸🇺🇿🇷🇺");
            rw2.add("About me ℹ️");
            rw2.add("Clear cache ♻️");
            rw3.add("Back ⬅️");
        } else if (language.equals(Language.UZBEK)) {
            rw1.add("Tilni o'zgartirish 🇺🇿🇷🇺🇺🇸");
            rw2.add("Men haqimda ℹ️");
            rw2.add("Cache tozalash ♻️");
            rw3.add("Orqaga ⬅️");
        } else {
            rw1.add("Изменить язык 🇷🇺🇺🇸🇺🇿");
            rw2.add("О себе ℹ️");
            rw2.add("Очистить кэш ♻️");
            rw3.add("Назад ⬅️");
        }

        markup.setKeyboard(List.of(rw1, rw2, rw3));

        return markup;
    }
}

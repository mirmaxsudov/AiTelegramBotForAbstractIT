package uz.abdurahmon.aitelegrambot.service.bot.inlineKeyBoards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import uz.abdurahmon.aitelegrambot.entity.User;

import java.util.List;

public interface InlineMarkup {
    default InlineKeyboardMarkup getInlineKeyboardMarkupToAskAnalyzeImage(User user) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton agree = new InlineKeyboardButton();
        InlineKeyboardButton disagree = new InlineKeyboardButton();
        InlineKeyboardButton back = new InlineKeyboardButton();

        switch (user.getLanguage()) {
            case UZBEK -> {
                agree.setText("Ha ✅");
                disagree.setText("Yoq ❌");
                back.setText("Ortga 🔙");
            }
            case ENGLISH -> {
                agree.setText("Yes ✅");
                disagree.setText("No ❌");
                back.setText("Back 🔙");
            }
            case RUSSIAN -> {
                agree.setText("Да ✅");
                disagree.setText("Нет ❌");
                back.setText("Назад 🔙");
            }
        }

        back.setCallbackData("BACK_TO_MAIN_PAGE");
        agree.setCallbackData("AGREE: " + user.getId());
        disagree.setCallbackData("DISAGREE: " + user.getId());

        markup.setKeyboard(List.of(List.of(agree, disagree), List.of(back)));

        return markup;
    }
}

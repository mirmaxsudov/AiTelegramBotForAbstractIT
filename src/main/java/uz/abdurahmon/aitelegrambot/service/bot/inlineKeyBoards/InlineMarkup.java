package uz.abdurahmon.aitelegrambot.service.bot.inlineKeyBoards;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import uz.abdurahmon.aitelegrambot.entity.Attachment;
import uz.abdurahmon.aitelegrambot.entity.Feedback;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;

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

    default InlineKeyboardMarkup getReplyKeyboardForFeedback(Long feedbackId, Language language) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton send = new InlineKeyboardButton();
        InlineKeyboardButton delete = new InlineKeyboardButton();

        switch (language) {
            case UZBEK -> {
                send.setText("Yuborish ✅");
                delete.setText("O'chirish ❌");
            }
            case ENGLISH -> {
                delete.setText("Delete ❌");
                send.setText("Send ✅");
            }
            default -> {
                delete.setText("Удалить ❌");
                send.setText("Отправить ✅");
            }
        }

        send.setCallbackData("SEND_FEEDBACK: " + feedbackId);
        delete.setCallbackData("DELETE_FEEDBACK: " + feedbackId);

        markup.setKeyboard(
                List.of(
                        List.of(send, delete)
                )
        );

        return markup;
    }

    default InlineKeyboardMarkup getFeedbackForGroup(Long feedbackId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton accept = new InlineKeyboardButton();
        InlineKeyboardButton decline = new InlineKeyboardButton();

        accept.setText("Accept ✅");
        decline.setText("Decline ❌");

        accept.setCallbackData("ACCEPT_FEEDBACK_FOR_GROUP: " + feedbackId);
        decline.setCallbackData("DECLINE_FEEDBACK_FOR_GROUP: " + feedbackId);

        markup.setKeyboard(List.of(List.of(accept, decline)));

        return markup;
    }

    default InlineKeyboardMarkup getInlineKeyboardMarkupToAskAnalyzeImage(Attachment attachment, Language language) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        InlineKeyboardButton send = new InlineKeyboardButton();
        InlineKeyboardButton delete = new InlineKeyboardButton();

        switch (language) {
            case UZBEK -> {
                send.setText("Yuborish ✅");
                delete.setText("O'chirish ❌");
            }
            case ENGLISH -> {
                delete.setText("Delete ❌");
                send.setText("Send ✅");
            }
            default -> {
                delete.setText("Удалить ❌");
                send.setText("Отправить ✅");
            }
        }

        send.setCallbackData("SEND_ATTACHMENT_TO_ANALYZE: " + attachment.getId());
        delete.setCallbackData("DELETE_ATTACHMENT: " + attachment.getId());

        markup.setKeyboard(List.of(List.of(send, delete)));
        return markup;
    }
}

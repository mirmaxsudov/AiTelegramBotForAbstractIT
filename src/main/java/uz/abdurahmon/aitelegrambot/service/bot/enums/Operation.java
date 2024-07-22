package uz.abdurahmon.aitelegrambot.service.bot.enums;

public enum Operation {
    // for login
    LOGIN_LANGUAGE,
    LOGIN_PHONE_NUMBER,
    TEXT_TO_SPEECH,

    // for analyze
    IMAGE_TO_TEXT_ASKED_IMG_LINK,
    TEXT_TO_TEXT_ASKED_TEXT,
    ASK_FEEDBACK_TEXT,
    IMAGE_TO_TEXT_ASKED_IMAGE, IMAGE_TO_SPEECH_ASKED_IMAGE;
}
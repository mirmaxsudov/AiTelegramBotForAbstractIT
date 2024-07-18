package uz.abdurahmon.aitelegrambot.entity.enums;

public enum Language {
    ENGLISH("English 🇺🇸"), UZBEK("Uzbek 🇺🇿"), RUSSIAN("Russian 🇷🇺");

    private final String name;

    Language(String name) {
        this.name = name;
    }

    public static Language getEnumByName(String name) {
        for (Language language : Language.values())
            if (language.name.equals(name))
                return language;

        return null;
    }
}
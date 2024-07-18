package uz.abdurahmon.aitelegrambot.entity.dto;

import lombok.*;
import uz.abdurahmon.aitelegrambot.entity.enums.Language;
import uz.abdurahmon.aitelegrambot.entity.enums.UserRole;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {
    private String phoneNumber;
    private Long chatId;
    private String firstName;
    private String lastName;
    private Language language;
    private UserRole userRole;
}
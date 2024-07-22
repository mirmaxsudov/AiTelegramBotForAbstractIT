package uz.abdurahmon.aitelegrambot.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attachment_seq")
    private Long id;
    private String link;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}
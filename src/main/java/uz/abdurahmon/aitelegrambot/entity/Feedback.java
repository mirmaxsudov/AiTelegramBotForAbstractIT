package uz.abdurahmon.aitelegrambot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "feedback_seq")
    private Long id;
    private String text;
    private boolean isSend;
    private boolean isRead;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;
}
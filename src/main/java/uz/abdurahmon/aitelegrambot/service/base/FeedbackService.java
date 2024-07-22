package uz.abdurahmon.aitelegrambot.service.base;

import uz.abdurahmon.aitelegrambot.entity.Feedback;

public interface FeedbackService {
    void save(Feedback feedback);

    Feedback getById(Long feedbackId);

    void deleteById(Long feedbackId);
}

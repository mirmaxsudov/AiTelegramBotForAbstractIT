package uz.abdurahmon.aitelegrambot.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.abdurahmon.aitelegrambot.entity.Feedback;
import uz.abdurahmon.aitelegrambot.repository.FeedbackRepository;
import uz.abdurahmon.aitelegrambot.service.base.FeedbackService;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {
    private final FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void save(Feedback feedback) {
        feedbackRepository.save(feedback);
    }

    @Override
    public Feedback getById(Long feedbackId) {
        return feedbackRepository.findById(feedbackId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteById(Long feedbackId) {
        feedbackRepository.deleteById(feedbackId);
    }
}

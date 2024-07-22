package uz.abdurahmon.aitelegrambot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.abdurahmon.aitelegrambot.entity.Attachment;
import uz.abdurahmon.aitelegrambot.entity.User;
import uz.abdurahmon.aitelegrambot.repository.AttachmentRepository;
import uz.abdurahmon.aitelegrambot.service.base.AttachmentService;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;

    @Override
    public Attachment save(String url, User user) {
        Attachment attachment = new Attachment();
        attachment.setLink(url);
        attachment.setUser(user);

        attachmentRepository.save(attachment);
        return attachment;
    }

    @Override
    public Attachment getById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId).orElse(null);
    }
}

package uz.abdurahmon.aitelegrambot.service.base;

import uz.abdurahmon.aitelegrambot.entity.Attachment;
import uz.abdurahmon.aitelegrambot.entity.User;

public interface AttachmentService {
    Attachment save(String url, User user);

    Attachment getById(Long attachmentId);
}

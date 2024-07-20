package uz.abdurahmon.aitelegrambot.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.abdurahmon.aitelegrambot.service.base.DownloadImgService;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DownloadImgServiceImpl implements DownloadImgService {
    private static final Logger log = LoggerFactory.getLogger(DownloadImgServiceImpl.class);

    @Override
    public String download(String imageUrl) {
        String destinationFile = "src/main/resources/files/";
        String full = destinationFile + UUID.randomUUID() + ".jpg";
        try (InputStream in = new BufferedInputStream(new URL(imageUrl).openStream());
             FileOutputStream out = new FileOutputStream(full)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(buffer, 0, 1024)) != -1)
                out.write(buffer, 0, bytesRead);

            return full;
        } catch (Exception e) {
            log.error("Error occurred: ", e);
            return null;
        }
    }
}

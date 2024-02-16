package com.chatbot.tele.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.request.SendVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoTranslationService {

    private TelegramBot bot;

    public VideoTranslationService(@Value("${telegram.bot.token}") String botToken) {
        this.bot = new TelegramBot(botToken);
    }
    private static final Logger logger = LoggerFactory.getLogger(VideoTranslationService.class);
    @Autowired
    private TelegramService telegramService;
    @Autowired
    private SpeechRecognitionService speechRecognitionService;

    @Autowired
    private TranslationService translationService;

    @Autowired
    private TextToSpeechService textToSpeechService;




    public void processVideo(String videoUrl, long chatId) {
        try {
            logger.info("Starting video processing for chatId: {}", chatId);
            File file = telegramService.getFile(videoUrl);
            String filePath = file.filePath();
            String mediaUrl = telegramService.getFullFilePath(filePath);
            String recognizedText = speechRecognitionService.recognizeSpeechFromMedia(mediaUrl, "video");
            logger.info("Speech recognized for chatId: {}", chatId);

            String translatedText = translationService.translateTextToKyrgyz(recognizedText);
            logger.info("Text translated for chatId: {}", chatId);

            Path audioPath = textToSpeechService.synthesizeSpeech(translatedText); // Пример использования speakerId = 1
            logger.info("Speech synthesized for chatId: {}", chatId);

            // Генерация пути к выходному файлу
            //Path outputPath = Files.createTempFile("merged-video-", ".mp4");
            Path outputPath = Files.createTempFile(Paths.get(System.getProperty("java.io.tmpdir")), "merged-video-", ".mp4");
            // Обновление вызова метода с корректным количеством и типами аргументов
            mergeAudioWithVideo(mediaUrl, audioPath.toString(), outputPath.toString(),chatId);
            logger.info("Audio merged with video for chatId: {}, outputPath: {}", chatId, outputPath);

            // Отправка сгенерированного видео пользователю
            // sendVideoToUser(chatId, outputPath.toString());
        } catch (Exception e) {
            logger.error("Error processing video for chatId: {}", chatId, e);
            // Обработка ошибок, например, отправка сообщения об ошибке пользователю
        }
    }

    private void mergeAudioWithVideo(String videoPath, String audioPath, String outputPath, Long chatId) throws IOException, InterruptedException {
        // Отправляем исполнение FFmpeg с настроенными входами и выходом
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-i", videoPath, "-i", audioPath,"-y", "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy", "-shortest", outputPath.toString());
        builder.inheritIO();
        Process process = builder.start();
        process.waitFor();

        sendVideoToUser(chatId, outputPath);
        System.out.println("Output video path: " + outputPath.toString());
    }

    // Метод для отправки сгенерированного видео пользователю
    // private void sendVideoToUser(long chatId, String videoPath) {...}
    public void sendVideoToUser(long chatId, String videoPath) {
        java.io.File videoFile = new java.io.File(videoPath);
        bot.execute(new SendVideo(chatId, videoFile));
    }
}

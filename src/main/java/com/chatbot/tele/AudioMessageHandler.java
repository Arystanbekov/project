package com.chatbot.tele;

import com.pengrad.telegrambot.model.File;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AudioMessageHandler {
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;

    @Autowired
    public AudioMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService, TelegramService telegramService) {
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
    }

    public void handleAudio(String fileId, long chatId) {
        try {
            // Fetching the file path from Telegram
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String audioUrl = telegramService.getFullFilePath(filePath);

            // Use the updated SpeechRecognitionService which accepts a URL
            String recognizedText = speechRecognitionService.recognizeSpeechFromAudio(audioUrl);
            if (recognizedText.isEmpty()) {
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
            } else {
                telegramService.sendMessage(chatId, "Распознанный текст: " + recognizedText);
            }
        } catch (Exception e) {
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке аудио.");
        }
    }
}

package com.chatbot.tele;

import com.pengrad.telegrambot.model.File;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AudioMessageHandler {
    // Existing fields
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;
    private final TranslationService translationService; // Add this line

    @Autowired
    public AudioMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService, TelegramService telegramService, TranslationService translationService) { // Update constructor
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
        this.translationService = translationService; // Initialize translation service
    }

    public void handleAudio(String fileId, long chatId) {
        try {
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String audioUrl = telegramService.getFullFilePath(filePath);
            String recognizedText = speechRecognitionService.recognizeSpeechFromAudio(audioUrl);

            if (recognizedText.isEmpty()) {
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
                return;
            }

            // Translate the recognized text to Kyrgyz
            String translatedText = translationService.translateTextToKyrgyz(recognizedText);
            telegramService.sendMessage(chatId, "Распознанный текст: " + translatedText); // Send the translated text

        } catch (Exception e) {
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке аудио.");
        }
    }
}

package com.chatbot.tele;

import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.request.InputFile;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import com.pengrad.telegrambot.model.request.InputFile;
@Service
public class AudioMessageHandler {
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;
    private final TranslationService translationService;
    private final TextToSpeechService textToSpeechService; // Reference to TextToSpeechService

    @Autowired
    public AudioMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService,
                               TelegramService telegramService, TranslationService translationService,
                               TextToSpeechService textToSpeechService) { // Include TextToSpeechService in constructor
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
        this.translationService = translationService;
        this.textToSpeechService = textToSpeechService; // Initialize TextToSpeechService
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

            String translatedText = translationService.translateTextToKyrgyz(recognizedText);
            int speakerId = 1; // Example speaker ID

            Response ttsResponse = textToSpeechService.sendTextForSpeech(translatedText, speakerId);

            if (!ttsResponse.isSuccessful()) {
                telegramService.sendMessage(chatId, "Не удалось преобразовать текст в речь.");
                return;
            }

            // Assuming responseBody is not null and contains the binary MP3 data
            ResponseBody responseBody = ttsResponse.body();
            if (responseBody != null) {
                // Create a temporary file to store the MP3
                Path tempFile = Files.createTempFile("tts-", ".mp3");
                Files.copy(responseBody.byteStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                // The file name for use in Telegram (Telegram requires filenames for audio)
                String tempFileName = "tts_audio.mp3";

                // Assuming the last parameter is for MIME type or can be used for a description
                String mimeType = "audio/mp3"; // Adjust based on actual requirements or API documentation

                // Corrected constructor usage with three parameters
                InputFile audioToSend = new InputFile(tempFile.toFile(), tempFileName, mimeType);

                // Send the audio message
                telegramService.getBot().execute(new com.pengrad.telegrambot.request.SendAudio(chatId, audioToSend.getFile()));

                // Cleanup the temporary file
                Files.deleteIfExists(tempFile);
            } else {
                telegramService.sendMessage(chatId, "Ответ от сервиса TTS был пустым.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке аудио.");
        }
    }
}

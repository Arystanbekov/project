package com.chatbot.tele;

import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.request.InputFile;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class AudioMessageHandler {
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;
    private final TranslationService translationService;
    private final TextToSpeechService textToSpeechService; // Reference to TextToSpeechService
    private final VideoToAudioService videoToAudioService;
    private static final Logger logger = LoggerFactory.getLogger(AudioMessageHandler.class);
    @Autowired
    public AudioMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService,
                               TelegramService telegramService, TranslationService translationService,
                               TextToSpeechService textToSpeechService, VideoToAudioService videoToAudioService) { // Include TextToSpeechService in constructor
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
        this.translationService = translationService;
        this.textToSpeechService = textToSpeechService; // Initialize TextToSpeechService
        this.videoToAudioService = videoToAudioService;
    }

    public void handleAudio(String fileId, long chatId) {
        logger.info("Handling audio for chat ID: {}, file ID: {}", chatId, fileId);
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
                telegramService.sendMessage(chatId, translatedText);
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
            logger.error("Error handling audio for chat ID: " + chatId, e);
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке аудио.");
        }
    }
    public void handleVideo(String fileId, long chatId) {
        logger.info("Handling video for chat ID: {}, file ID: {}", chatId, fileId);
        try {
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String videoUrl = telegramService.getFullFilePath(filePath);
            logger.info("Video URL obtained: {}", videoUrl);
            Path audioPath = videoToAudioService.extractAudioFromVideo(videoUrl);
            logger.info("Audio extracted and saved to path: {}", audioPath);
            String audioUrl = audioPath.toString(); // Передайте URL или путь к аудиофайлу далее, как это необходимо
            // Далее следуйте логике обработки аудиофайла, как в handleAudio
            String recognizedText = speechRecognitionService.recognizeSpeechFromAudioLocal(audioPath);
            if (recognizedText.isEmpty()) {
                logger.warn("No text recognized from the audio for chat ID: {}", chatId);
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
                return;
            }
            logger.info("Recognized text: {}", recognizedText);
            String translatedText = translationService.translateTextToKyrgyz(recognizedText);
            logger.info("Text translated to Kyrgyz: {}", translatedText);
            int speakerId = 1;

            Response ttsResponse = textToSpeechService.sendTextForSpeech(translatedText, speakerId);

            if (!ttsResponse.isSuccessful()) {
                logger.error("Failed to convert text to speech for chat ID: {}", chatId);
                telegramService.sendMessage(chatId, "Не удалось преобразовать текст в речь.");
                return;
            }

            // Assuming responseBody is not null and contains the binary MP3 data
            ResponseBody responseBody = ttsResponse.body();
            if (responseBody != null) {
                // Create a temporary file to store the MP3
                Path tempFile = Files.createTempFile("tts-", ".mp3");
                Files.copy(responseBody.byteStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("TTS audio saved to temporary file: {}", tempFile);

                // The file name for use in Telegram (Telegram requires filenames for audio)
                String tempFileName = "tts_audio.mp3";

                // Assuming the last parameter is for MIME type or can be used for a description
                String mimeType = "audio/mp3"; // Adjust based on actual requirements or API documentation

                // Corrected constructor usage with three parameters
                InputFile audioToSend = new InputFile(tempFile.toFile(), tempFileName, mimeType);

                // Send the audio message
                telegramService.getBot().execute(new com.pengrad.telegrambot.request.SendAudio(chatId, audioToSend.getFile()));
                logger.info("Audio message sent to chat ID: {}", chatId);
                // Cleanup the temporary file
                Files.deleteIfExists(tempFile);
                logger.info("Temporary file deleted: {}", tempFile);
            } else {
                logger.warn("TTS response body was null for chat ID: {}", chatId);
                telegramService.sendMessage(chatId, "Ответ от сервиса TTS был пустым.");
            }
        } catch (Exception e) {
            logger.error("Error handling video for chat ID: " + chatId, e);
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке видео.");
        }
    }
}

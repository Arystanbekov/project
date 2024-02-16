package com.chatbot.tele.service;
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

@Service
public class MediaMessageHandler {
    private final OkHttpClient httpClient;
    private final SpeechRecognitionService speechRecognitionService;
    private final TelegramService telegramService;
    private final TranslationService translationService;
    private final TextToSpeechService textToSpeechService;

    @Autowired
    public MediaMessageHandler(OkHttpClient httpClient, SpeechRecognitionService speechRecognitionService,
                               TelegramService telegramService, TranslationService translationService,
                               TextToSpeechService textToSpeechService) {
        this.httpClient = httpClient;
        this.speechRecognitionService = speechRecognitionService;
        this.telegramService = telegramService;
        this.translationService = translationService;
        this.textToSpeechService = textToSpeechService;
    }

    public void handleMedia(String fileId, long chatId, String mediaType) { // Добавлен параметр mediaType
        try {
            File file = telegramService.getFile(fileId);
            String filePath = file.filePath();
            String mediaUrl = telegramService.getFullFilePath(filePath);
            String recognizedText = speechRecognitionService.recognizeSpeechFromMedia(mediaUrl, mediaType); // Обновлено для поддержки видео

            if (recognizedText.isEmpty()) {
                telegramService.sendMessage(chatId, "Не удалось распознать текст.");
                return;
            }

            String translatedText = translationService.translateTextToKyrgyz(recognizedText);
            Response ttsResponse = textToSpeechService.sendTextForSpeech(translatedText, 1);

            if (!ttsResponse.isSuccessful()) {
                telegramService.sendMessage(chatId, "Не удалось преобразовать текст в речь.");
                return;
            }

            ResponseBody responseBody = ttsResponse.body();
            if (responseBody != null) {
                Path tempFile = Files.createTempFile("tts-", ".mp3");
                Files.copy(responseBody.byteStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                InputFile audioToSend = new InputFile(tempFile.toFile(), "tts_audio.mp3", "audio/mp3");
                telegramService.getBot().execute(new com.pengrad.telegrambot.request.SendAudio(chatId, audioToSend.getFile()));
                Files.deleteIfExists(tempFile);
            } else {
                telegramService.sendMessage(chatId, "Ответ от сервиса TTS был пустым.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Произошла ошибка при обработке медиа.");
        }
    }



    private void mergeAudioWithVideo(String videoUrl, String audioPath, String outputPath) throws IOException, InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-i", videoUrl, "-i", audioPath, "-c:v", "copy", "-map", "0:v:0", "-map", "1:a:0", "-shortest", outputPath);
        Process process = builder.start();
        process.waitFor();
        // Проверка успешности выполнения команды
    }
}

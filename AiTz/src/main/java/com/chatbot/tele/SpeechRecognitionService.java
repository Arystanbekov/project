package com.chatbot.tele;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class SpeechRecognitionService {

    private final OkHttpClient client;
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecognitionService.class);

    @Autowired
    public SpeechRecognitionService(OkHttpClient client) {
        this.client = client;
    }
    public String recognizeSpeechFromAudioLocal(Path audioPath) throws IOException, InterruptedException {
        // Предполагается, что вы уже установили Whisper и он доступен в вашем PATH.
        // Измените "whisper" на полный путь к исполняемому файлу, если это необходимо.
        String command = String.format("whisper %s --model small --task translate --language en", audioPath.toString());


        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));

        processBuilder.redirectErrorStream(true); // Для логирования ошибок и стандартного вывода в одном потоке

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean transcriptionStarted = false;
            while ((line = reader.readLine()) != null) {
                // Ищем строки с временными метками и текстом, игнорируем всё остальное
                if (line.matches("\\[\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}\\.\\d{3}\\].*")) {
                    transcriptionStarted = true; // Начало транскрипции
                    String textLine = line.substring(line.indexOf(']') + 2); // Убираем временные метки
                    output.append(textLine).append(" ");
                } else if (transcriptionStarted) {
                    // Если встретилась строка без временной метки после начала транскрипции, прекращаем чтение
                    break;
                }
            }
        }

        int exitVal = process.waitFor();
        if (exitVal != 0) {
            logger.error("Whisper failed with exit code " + exitVal);
            return "";
        }

        Files.deleteIfExists(audioPath); // Удаление временного аудиофайла
        logger.info(output.toString());
        return output.toString();
    }
    public String recognizeSpeechFromAudio(String audioUrl) throws IOException, InterruptedException {
        Path audioPath = downloadAudio(audioUrl);

        String command = String.format("whisper %s --model small --task translate --language en", audioPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
        processBuilder.redirectErrorStream(true); // Для логирования ошибок и стандартного вывода в одном потоке

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean transcriptionStarted = false;
            while ((line = reader.readLine()) != null) {
                // Ищем строки с временными метками и текстом, игнорируем всё остальное
                if (line.matches("\\[\\d{2}:\\d{2}\\.\\d{3} --> \\d{2}:\\d{2}\\.\\d{3}\\].*")) {
                    transcriptionStarted = true; // Начало транскрипции
                    String textLine = line.substring(line.indexOf(']') + 2); // Убираем временные метки
                    output.append(textLine).append(" ");
                } else if (transcriptionStarted) {
                    // Если встретилась строка без временной метки после начала транскрипции, прекращаем чтение
                    break;
                }
            }
        }

        int exitVal = process.waitFor();
        if (exitVal != 0) {
            logger.error("Whisper failed with exit code " + exitVal);
            return "";
        }

        Files.deleteIfExists(audioPath); // Удаление временного аудиофайла
        logger.info(output.toString().trim());
        return output.toString().trim();
    }


    private Path downloadAudio(String audioUrl) throws IOException {
        Request request = new Request.Builder().url(audioUrl).build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to download audio from " + audioUrl);
        }

        Path tempAudioFile = Files.createTempFile("downloaded-audio", ".mp3");
        Files.copy(response.body().byteStream(), tempAudioFile, StandardCopyOption.REPLACE_EXISTING);

        response.close();

        return tempAudioFile;
    }
}

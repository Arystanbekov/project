package com.chatbot.tele.service;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SpeechRecognitionService {

    private String apiKey;
    private OkHttpClient client;

    @Autowired
    public SpeechRecognitionService(@Value("${openai.api.key}") String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.client = client;
    }

    public String recognizeSpeechFromMedia(String mediaUrl, String mediaType) throws IOException {
        String contentType = "video".equals(mediaType) ? "video/mp4" : "audio/ogg";

        Request downloadRequest = new Request.Builder().url(mediaUrl).build();
        Response downloadResponse = client.newCall(downloadRequest).execute();
        if (!downloadResponse.isSuccessful()) {
            throw new IOException("Failed to download file: " + downloadResponse);
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "media." + (mediaType.equals("video") ? "mp4" : "oga"),
                        RequestBody.create(downloadResponse.body().bytes(), MediaType.parse(contentType)))
                .addFormDataPart("model", "whisper-1")
                .build();

        Request whisperRequest = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response whisperResponse = client.newCall(whisperRequest).execute()) {
            if (!whisperResponse.isSuccessful()) {
                throw new IOException("Unexpected code from Whisper API: " + whisperResponse);
            }

            // Изменено для корректного извлечения распознанного текста из ответа
            JSONObject jsonResponse = new JSONObject(whisperResponse.body().string());
            // Вместо извлечения массива 'transcriptions', прямо получаем текст из ключа 'text'
            String recognizedText = jsonResponse.getString("text");
            return recognizedText;
        } catch (Exception e) { // Ловим все исключения, включая ошибки разбора JSON
            throw new IOException("Error processing Whisper API response: " + e.getMessage(), e);
        }
    }
}

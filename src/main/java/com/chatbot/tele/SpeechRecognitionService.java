package com.chatbot.tele;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SpeechRecognitionService {

    // Assuming apiKey and client are initialized elsewhere
    private String apiKey; // Your OpenAI API key
    private OkHttpClient client; // Reuse your OkHttpClient instance
    @Autowired
    public SpeechRecognitionService(@Value("${openai.api.key}") String apiKey, OkHttpClient client) {
        this.apiKey = apiKey;
        this.client = client;
    }

    public String recognizeSpeechFromAudio(String audioUrl) throws IOException {
        // First, download the audio file from the URL
        Request downloadRequest = new Request.Builder().url(audioUrl).build();
        Response downloadResponse = client.newCall(downloadRequest).execute();
        if (!downloadResponse.isSuccessful()) {
            throw new IOException("Failed to download file: " + downloadResponse);
        }

        // Assuming the response body is not null and the API you're calling supports byte streams
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "audio.oga",
                        RequestBody.create(downloadResponse.body().bytes(), MediaType.parse("audio/ogg")))
                .addFormDataPart("model", "whisper-1")
                .build();

        Request whisperRequest = new Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response whisperResponse = client.newCall(whisperRequest).execute()) {
            if (!whisperResponse.isSuccessful()) {
                throw new IOException("Unexpected code " + whisperResponse);
            }

            // Process the response from Whisper
            String responseBody = whisperResponse.body().string();
            // Assuming responseBody is JSON and contains a "text" field with the transcription
            return responseBody; // Modify as necessary to parse and return the actual transcription
        }
    }
}

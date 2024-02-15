package com.chatbot.tele;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public TranslationService translationService(@Value("${openai.api.key}") String apiKey, OkHttpClient client) {
        return new TranslationService(apiKey, client);
    }
}

package com.chatbot.tele;

import com.chatbot.tele.service.AuthService;
import com.chatbot.tele.service.MediaMessageHandler;
import com.chatbot.tele.service.TelegramService;
import com.chatbot.tele.service.VideoTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.pengrad.telegrambot.UpdatesListener;

@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramService telegramService;
    private final MediaMessageHandler mediaMessageHandler;
    private final AuthService authService;
    private final VideoTranslationService videoTranslationService;

    @Autowired
    public BotRunner(TelegramService telegramService, MediaMessageHandler mediaMessageHandler, AuthService authService, VideoTranslationService videoTranslationService) {
        this.telegramService = telegramService;
        this.mediaMessageHandler = mediaMessageHandler;
        this.authService = authService;
        this.videoTranslationService = videoTranslationService;
    }

    @Override
    public void run(String... args) {
        telegramService.getBot().setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null && update.message().chat() != null) {
                    long chatId = update.message().chat().id();
                    Long telegramUserId = update.message().from().id();

                    // Проверка наличия текста в сообщении для избежания NullPointerException
                    String text = update.message().text();
                    if (text != null) {
                        switch (text) {
                            case "/start":
                                if (!authService.authenticate(telegramUserId)) {
                                    telegramService.sendMessage(chatId, "Привет! Пожалуйста, зарегистрируйтесь с помощью команды /register.");
                                } else {
                                    telegramService.sendMessage(chatId, "Вы уже зарегистрированы. Добро пожаловать обратно!");
                                }
                                break;
                            case "/register":
                                authService.registerOrUpdateUser(telegramUserId, true);
                                telegramService.sendMessage(chatId, "Вы успешно зарегистрированы. Теперь вы можете использовать все функции бота.");
                                break;
                            // Добавьте обработку других команд здесь, если необходимо.
                        }
                    }

                    // Проверка аутентификации перед обработкой медиа
                    if (authService.authenticate(telegramUserId)) {
                        if (update.message().voice() != null) {
                            mediaMessageHandler.handleMedia(update.message().voice().fileId(), chatId, "audio");
                        } else if (update.message().audio() != null) {
                            mediaMessageHandler.handleMedia(update.message().audio().fileId(), chatId, "audio");
                        } else if (update.message().video() != null) {
                            videoTranslationService.processVideo(update.message().video().fileId(), chatId);
                        }
                    } else {
                        telegramService.sendMessage(chatId, "Пожалуйста, зарегистрируйтесь с помощью команды /register для доступа к функциям.");
                    }
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}

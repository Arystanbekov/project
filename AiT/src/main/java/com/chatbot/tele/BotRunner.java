package com.chatbot.tele;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.pengrad.telegrambot.UpdatesListener;

@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramService telegramService;
    private final AudioMessageHandler audioMessageHandler;

    @Autowired
    public BotRunner(TelegramService telegramService, AudioMessageHandler audioMessageHandler) {
        this.telegramService = telegramService;
        this.audioMessageHandler = audioMessageHandler;
    }

    @Override
    public void run(String... args) throws Exception {
        telegramService.getBot().setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null) {
                    long chatId = update.message().chat().id();
                    if (update.message().voice() != null) {
                        String fileId = update.message().voice().fileId();
                        audioMessageHandler.handleAudio(fileId, chatId);
                    } else if (update.message().audio() != null) {
                        String fileId = update.message().audio().fileId();
                        audioMessageHandler.handleAudio(fileId, chatId);
                    }
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}

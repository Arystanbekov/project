package com.chatbot.tele;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.pengrad.telegrambot.UpdatesListener;

@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramService telegramService;
    private final MediaMessageHandler mediaMessageHandler;

    @Autowired
    public BotRunner(TelegramService telegramService, MediaMessageHandler mediaMessageHandler) {
        this.telegramService = telegramService;
        this.mediaMessageHandler = mediaMessageHandler;
    }

    @Override
    public void run(String... args) {
        telegramService.getBot().setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null && update.message().chat() != null) {
                    long chatId = update.message().chat().id();
                    if (update.message().voice() != null) {
                        mediaMessageHandler.handleMedia(update.message().voice().fileId(), chatId, "audio");
                    } else if (update.message().audio() != null) {
                        mediaMessageHandler.handleMedia(update.message().audio().fileId(), chatId, "audio");
                    } else if (update.message().video() != null) {
                        mediaMessageHandler.handleMedia(update.message().video().fileId(), chatId, "video");
                    }
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}

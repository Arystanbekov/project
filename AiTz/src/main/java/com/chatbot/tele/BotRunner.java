package com.chatbot.tele;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.pengrad.telegrambot.UpdatesListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class BotRunner implements CommandLineRunner {

    private final TelegramService telegramService;
    private final AudioMessageHandler audioMessageHandler;
    private static final Logger logger = LoggerFactory.getLogger(BotRunner.class);
    @Autowired
    public BotRunner(TelegramService telegramService, AudioMessageHandler audioMessageHandler) {
        this.telegramService = telegramService;
        this.audioMessageHandler = audioMessageHandler;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Bot is starting");
        telegramService.getBot().setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null) {
                    long chatId = update.message().chat().id();
                    logger.info("Received a message from chat ID: {}", chatId);
                    if (update.message().voice() != null) {
                        String fileId = update.message().voice().fileId();
                        logger.info("Handling an audio message with file ID: {}", fileId);
                        audioMessageHandler.handleAudio(fileId, chatId);
                    } else if (update.message().audio() != null) {
                        String fileId = update.message().audio().fileId();
                        logger.info("Handling an audio message with file ID: {}", fileId);
                        audioMessageHandler.handleAudio(fileId, chatId);
                    } else if (update.message().video() != null) {
                        String fileId = update.message().video().fileId();
                        logger.info("Handling a video message with file ID: {}", fileId);
                        audioMessageHandler.handleVideo(fileId, chatId);
                    }
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}

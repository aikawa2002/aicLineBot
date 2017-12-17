package jp.co.ricoh.jrits;

import java.io.IOException;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.postback.PostbackContent;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

@LineMessageHandler
public class MessageHandler {

    private final ReplyMessageHandler replyMessageHandler;

    public MessageHandler(ReplyMessageHandler replyMessageHandler) {
        super();
        this.replyMessageHandler = replyMessageHandler;
    }

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws IOException {
        System.out.println("event: " + event);
        BotApiResponse response = replyMessageHandler.reply(event);
        System.out.println("Sent messages: " + response);
    }

    @EventMapping
    public void handleImageMessage(MessageEvent<ImageMessageContent> event) throws IOException {
        BotApiResponse response = replyMessageHandler.replyImage(event);
    }

    @EventMapping
    public void handleAudioMessage(MessageEvent<AudioMessageContent> event) throws IOException {
        BotApiResponse response = replyMessageHandler.replyAudio(event);
    }

    @EventMapping
    public void handlePostBack(PostbackEvent event) throws IOException {
        System.out.println("postBackEvent: " + event.getPostbackContent().getData());
        replyMessageHandler.discoveryTraining(event.getPostbackContent().getData());
    }

    @EventMapping
    public void defaultMessageEvent(Event event) {
        System.out.println("event: " + event);
    }


}
package jp.co.ricoh.jrits;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ibm.watson.developer_cloud.conversation.v1.Conversation;
import com.ibm.watson.developer_cloud.conversation.v1.model.Context;
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import com.ibm.watson.developer_cloud.personality_insights.v3.model.ProfileOptions.AcceptLanguage;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassResult;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImage;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifiedImages;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyOptions;
import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.AudioMessageContent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.profile.UserProfileResponse;
import com.linecorp.bot.model.response.BotApiResponse;

import jp.co.ricoh.jrits.watson.WatsonDiscovery;
import okhttp3.ResponseBody;
import retrofit2.Response;


/**
 *
 * @author M.Aikawa
 *
 */
@Service
public class ReplyMessageHandler {

  private final LineMessagingService lineMessagingService;
  private Conversation service;
  private Context context = null;
  private VisualRecognition visualService;
  private TemplateMessage templete;
  private SpeechToText sttService;
  private RecognizeOptions options;
  private WatsonDiscovery discovery;

  public ReplyMessageHandler(LineMessagingService lineMessagingService) {
	  super();
	  service = new Conversation(Conversation.VERSION_DATE_2017_05_26);
	  service.setUsernameAndPassword("00093565-bfad-47e9-810c-2dc833c837bd", "wobY7ZI5eVeW");
	  visualService = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
	  visualService.setApiKey("3ba8013ffffecb635283347653e29e61558e67e1");
	  sttService = new SpeechToText();
	  sttService.setUsernameAndPassword("9397f5eb-f05c-4e58-9ac5-419c1b240005", "dq8i5SHBWXxQ");
	  options = new RecognizeOptions.Builder().model("ja-JP_BroadbandModel").contentType("audio/mpeg").build();
	  discovery = new WatsonDiscovery();

	  this.lineMessagingService = lineMessagingService;

	  //Action yes = new PostbackAction("はい","PostBackYes");
	  //Action no = new PostbackAction("いいえ","PostBackNo");
  }

  @SuppressWarnings("null")
  public BotApiResponse reply(MessageEvent<TextMessageContent> event) throws IOException {

  String receivedMessage = event.getMessage().getText();
  String replyToken = event.getReplyToken();
  BotApiResponse resp = null;

  if (receivedMessage.equals("あ")) {
      List<Message> messages = new ArrayList<>();
      List<Action> actionList = new ArrayList<>();
      actionList.add(new MessageAction("今日のご飯何？", "今日のご飯何？"));
      actionList.add(new MessageAction("机は何ゴミですか？", "机は何ゴミですか？"));

      templete = new TemplateMessage("メッセージ候補",new ConfirmTemplate("メッセージ候補",actionList));
      messages.add(templete);
      resp = lineMessagingService
              .replyMessage(new ReplyMessage(replyToken, messages))
              .execute()
              .body();
  } else if (receivedMessage.contains("ごはん") || receivedMessage.contains("飯")) {
	  Response<UserProfileResponse> profile = lineMessagingService.getProfile(event.getSource().getUserId()).execute();
	  resp = discover(profile.body().getDisplayName(),receivedMessage, replyToken);
  } else {
	  resp = execute(receivedMessage, replyToken);
  }

  return resp;
  }

  @SuppressWarnings("null")
  public BotApiResponse replyAudio(MessageEvent<AudioMessageContent> event) throws IOException {
	  retrofit2.Response<ResponseBody> response =lineMessagingService.getMessageContent(event.getMessage().getId()).execute();
		if (response.isSuccessful()) {
		    ResponseBody content = response.body();
		    Path path = Files.createTempFile(event.getMessage().getId(), ".m4a");
		    Files.copy(content.byteStream(), path,StandardCopyOption.REPLACE_EXISTING);
		    System.out.println("Audio file:" + path.toAbsolutePath().toString());

		    File audio = new File(path.toAbsolutePath().toString());
		    SpeechResults transcript = sttService.recognize(audio,options).execute();

		    System.out.println(transcript.getResults().get(0));
		}

	  String replyToken = event.getReplyToken();
	  return null;
	  //return execute(receivedMessage, replyToken);
  }

  @SuppressWarnings("null")
  public BotApiResponse replyImage(MessageEvent<ImageMessageContent> event) throws IOException {
	  String replyToken = event.getReplyToken();

      String receiveMessage = null;
	  retrofit2.Response<ResponseBody> response =lineMessagingService.getMessageContent(event.getMessage().getId()).execute();
		if (response.isSuccessful()) {
		    ResponseBody content = response.body();
		    Path path = Files.createTempFile(event.getMessage().getId(), ".jpeg");
		    Files.copy(content.byteStream(), path,StandardCopyOption.REPLACE_EXISTING);

		    System.out.println("Classify an image:" + path.toAbsolutePath().toString());

		    ClassifyOptions options = new ClassifyOptions.Builder()
		    		.acceptLanguage(AcceptLanguage.JA)
		    	    .imagesFile(new File(path.toAbsolutePath().toString()))
		    	    .build();

		    ClassifiedImages result = visualService.classify(options).execute();
		    List<ClassifiedImage> list = result.getImages();
		    Float score = (float) 0;
		    receiveMessage = null;
		    for (ClassResult classResult:list.get(0).getClassifiers().get(0).getClasses()) {
		    	if (classResult.getTypeHierarchy() != null) {
			    	if (classResult.getScore() > score) {
			    		score = classResult.getScore();
			    		receiveMessage = classResult.getClassName();
			    	}
		    	}
		    }
		     System.out.println("result:" + receiveMessage);
		     System.out.println(result);
		} else {
		    System.out.println(response.code() + " " + response.message());
		}

      return execute(receiveMessage, replyToken);

  }

  public void discoveryTraining(String messages) {
	  String[] message = messages.split(":");
	  discovery.training(message[2], message[0], Integer.parseInt(message[1]));
  }

  private BotApiResponse execute(String receivedMessage, String replyToken) throws IOException {

	  // sync
	  MessageOptions newMessageOptions = new MessageOptions.Builder()
				    .workspaceId("66dcb959-7e33-4447-af48-6f8d8ffcb4a3")
				    .input(new InputData.Builder(receivedMessage).build())
				    .context(context)
				    .build();
	  MessageResponse response = service.message(newMessageOptions).execute();

	  System.out.println(response);

	  List<Message> messages = new ArrayList<>();
	  Map<String,Object> map = response.getOutput();
	  context = response.getContext();
	  String rep = map.get("text").toString();

	  messages.add(new TextMessage(rep.replace("[", "").replace("]", "")));
	  if (rep.contains("粗大ごみ")) {
		  messages.add(new TextMessage("粗大ごみは、処理手数料納付券が必要です。"));
		  messages.add(new ImageMessage("https://stat.ameba.jp/user_images/20170801/20/watashikoso/e6/83/j/o1960110213995587369.jpg","https://stat.ameba.jp/user_images/20170801/20/watashikoso/e6/83/j/o1960110213995587369.jpg"));
	  }

	  return lineMessagingService
	  .replyMessage(new ReplyMessage(replyToken, messages))
	  .execute()
	  .body();
  }

  private BotApiResponse discover(String name,String receivedMessage, String replyToken) throws IOException {
	  List<Message> messages = new ArrayList<>();
	  String foods = getFoods();
	  messages.add(new TextMessage("今日は"+ foods +"で何か作ります。"));
	  messages.add(new TextMessage("どれが好き？"));
	  templete = discovery.execute(name + " " +  foods);
	  messages.add(templete);

	  return lineMessagingService
	  .replyMessage(new ReplyMessage(replyToken, messages))
	  .execute()
	  .body();
  }

  private String getFoods() {
      int ran = (int)(Math.random()*10);
      return foods[ran];
  }

  String[] foods = {
		     "じゃがいも にんじん"
			,"あさり 味噌"
			,"牛肉 チーズ"
			,"卵 鮭"
			,"玉ねぎ 豚肉"
			,"れんこん 豆腐"
			,"長ねぎ ぶり"
			,"里いも 枝豆"
			,"大豆 明太子"
			,"ツナ マヨネーズ"
  			};


}
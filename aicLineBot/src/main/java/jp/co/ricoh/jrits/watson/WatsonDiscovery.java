package jp.co.ricoh.jrits.watson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.discovery.v1.model.AddTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.DeleteAllTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.DeleteTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.GetTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.ListTrainingDataOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.ListTrainingDataOptions.Builder;
import com.ibm.watson.developer_cloud.discovery.v1.model.QueryOptions;
import com.ibm.watson.developer_cloud.discovery.v1.model.TrainingDataSet;
import com.ibm.watson.developer_cloud.discovery.v1.model.TrainingExample;
import com.ibm.watson.developer_cloud.discovery.v1.model.TrainingQuery;
import com.ibm.watson.developer_cloud.discovery.v1.model.UpdateTrainingExampleOptions;
import com.ibm.watson.developer_cloud.service.exception.ConflictException;
import com.linecorp.bot.model.action.Action;
import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;

public class WatsonDiscovery {
	static final String environmentId = "a73786a3-d859-4812-86fe-b05b6f5295d1";
	static final String collectionId = "ed9b5772-9580-4761-b8b4-6a76fefdbae6";
	static final String configurationId = "a89da7f2-82f1-4def-a214-3fd69e28b829";
	static final String documentId = "{document_id}";
	Discovery discovery;

	public WatsonDiscovery() {
 		discovery = new Discovery("2016-12-15");

		discovery.setEndPoint("https://gateway.watsonplatform.net/discovery/api");
		discovery.setUsernameAndPassword("df054f31-fc95-49d8-b08d-16d540ea40c8", "wC7Wlm3VWfyK");
	}

	public TemplateMessage execute(String query) {

        QueryOptions.Builder queryBuilder = new QueryOptions.Builder(environmentId, collectionId);
        queryBuilder.naturalLanguageQuery(query);
		queryBuilder.passages(true);
		queryBuilder.highlight(false);
		queryBuilder.deduplicate(false);
		queryBuilder.count(5);
        com.ibm.watson.developer_cloud.discovery.v1.model.QueryResponse queryResponse = discovery.query(queryBuilder.build()).execute();

        AddTrainingDataOptions.Builder addTrainigOptions = new AddTrainingDataOptions.Builder(environmentId, collectionId);
        addTrainigOptions.naturalLanguageQuery(query);
        //addTrainigOptions.naturalLanguageQuery(human + " " +QUERY);


        List<CarouselColumn> carouselActionList = new ArrayList<>();
        for(Map<String, Object> response:queryResponse.getResults()){
				String docId = (String)response.get("id");
				String html = (String)response.get("html");
				@SuppressWarnings("unchecked")
				Map<String,Object> extracted_metadata = (Map<String,Object>)response.get("extracted_metadata");
				String title = (String)extracted_metadata.get("title");
				String[] titles = title.split(" ");
			  Action yes = new PostbackAction("好き",docId +":10:"+query);
			  Action no = new PostbackAction("嫌い",docId +":0:"+query);
			  List<Action> actionList = new ArrayList<>();
			  actionList.add(yes);
			  actionList.add(no);
			  carouselActionList.add(new CarouselColumn(
					  			getThumbnailURL(html),
					  			titles[0],
					  			titles[1],
				  				actionList));
        }

 	    return new TemplateMessage("好きな料理を選んでください",new CarouselTemplate(carouselActionList));

	}

	public void training(String naturalLanguageQuery,String documentId,int relevance) {

        TrainingExample trainingExample = new TrainingExample();
        trainingExample.setDocumentId(documentId);
        trainingExample.setRelevance(relevance);

    	//DeleteAllTrainingDataOptions.Builder deleteAllTrainingDataOptions = new DeleteAllTrainingDataOptions.Builder(environmentId, collectionId);
    	//discovery.deleteAllTrainingData(deleteAllTrainingDataOptions.build());

		String queryId = null;
		String exampleId = null;
		List<TrainingExample> updateQuery = null;
		ListTrainingDataOptions.Builder listTrainingDataOptions = new ListTrainingDataOptions.Builder(environmentId, collectionId);
    	TrainingDataSet dataSet = discovery.listTrainingData(listTrainingDataOptions.build()).execute();
    	List<TrainingQuery> queries = dataSet.getQueries();
    	for(TrainingQuery query:queries) {
    		if (query.getNaturalLanguageQuery().equals(naturalLanguageQuery)) {
    			updateQuery = query.getExamples();
    			queryId = query.getQueryId();
    	    	for(TrainingExample example:query.getExamples()) {
    	    		if (example.getDocumentId().equals(documentId)) {
    	    			if (example.getRelevance().intValue() == relevance) return;
    	    			exampleId = documentId;
    	    			break;
    	    		}
    	    	}
    	    	break;
    		}
    	}

    	if (queryId == null) {
            AddTrainingDataOptions.Builder addTrainigOptions = new AddTrainingDataOptions.Builder(environmentId, collectionId);
            addTrainigOptions.naturalLanguageQuery(naturalLanguageQuery);
            addTrainigOptions.addExamples(trainingExample);
        	discovery.addTrainingData(addTrainigOptions.build()).execute();
    	} else {

    		if (exampleId == null) {
            	updateQuery.add(trainingExample);

    			DeleteTrainingDataOptions.Builder delOption = new DeleteTrainingDataOptions.Builder(environmentId, collectionId,queryId);
            	discovery.deleteTrainingData(delOption.build()).execute();

                AddTrainingDataOptions.Builder addTrainigOptions = new AddTrainingDataOptions.Builder(environmentId, collectionId);
                addTrainigOptions.naturalLanguageQuery(naturalLanguageQuery);
                addTrainigOptions.examples(updateQuery);
            	discovery.addTrainingData(addTrainigOptions.build()).execute();

    		} else {
            	UpdateTrainingExampleOptions.Builder updateTrainigOptions = new UpdateTrainingExampleOptions.Builder(environmentId, collectionId,queryId,exampleId);
            	updateTrainigOptions.relevance(relevance);
            	discovery.updateTrainingExample(updateTrainigOptions.build()).execute();
    		}
    	}
	}

	private String getThumbnailURL(String html) {
        Pattern pattern = Pattern.compile("class=\"outline-img\" src=\"(.+?x=450)\"");
        Matcher matcher = pattern.matcher(html);
        if(matcher.find()) {
            return matcher.group(1);   // 1番目のグループにマッチした文字列を取り出す
        }
        return null;
	}

}

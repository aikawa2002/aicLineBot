package jp.co.ricoh.jrits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class LineBotApplication {
	private static ConfigurableApplicationContext context = null;

    public static void main(String[] args) {
        SpringApplication.run(LineBotApplication.class, args);
    }
    public static <T> T getBean(Class<T> clazz){
        return getContext().getBean(clazz);
    }

    private static ConfigurableApplicationContext getContext(){
        if(context == null){
            String args[] = new String[1];
            args[0] = "dummy";
            context = SpringApplication.run(LineBotApplication.class, args);
        }
        return context;
    }
 }
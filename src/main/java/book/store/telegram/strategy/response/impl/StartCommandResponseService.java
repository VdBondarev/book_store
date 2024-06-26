package book.store.telegram.strategy.response.impl;

import book.store.telegram.strategy.response.AdminResponseService;
import org.springframework.stereotype.Service;

@Service
public class StartCommandResponseService
        implements AdminResponseService {
    private static final String START = "/start";

    @Override
    public String getMessage(String text) {
        return """
                ***
                Hello, this is a book store bot.
                
                It is created for admins only.
                
                To find out more, click this button:
                /help
                ***
                """;
    }

    @Override
    public boolean isApplicable(String text) {
        return text.equalsIgnoreCase(START);
    }
}

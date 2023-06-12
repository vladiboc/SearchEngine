package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import searchengine.config.JsoupConnectionValues;
import searchengine.model.entities.IndexedPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class WebPage {

    private static JsoupConnectionValues valuesFromConfig;
    private static final String pathREGEX = "(\\/[0-9A-Za-z-_]*)+";
    private List<IndexedPage> subPages = new ArrayList<>();
    private IndexedPage page;

    public WebPage(IndexedPage page) {
        this.page = page;
        String currentPageUrl = page.getSite().getUrl() + page.getPath();
        try {
            Connection connection = Jsoup.connect(currentPageUrl)
                .userAgent(valuesFromConfig.getUserAgent())
                .referrer(valuesFromConfig.getReferrer());
            Document jsoupDocument = connection.get();
            page.setHttpResponseCode(connection.response().statusCode());
            String content = connection.response().body();
            content.replace("'", "\\\'");
            page.setContent(content);
            if (connection.response().statusCode() == HttpStatus.OK.value()) {
                parseSubPages(jsoupDocument);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseSubPages(Document jsoupDocument) {
        if (jsoupDocument == null || jsoupDocument.toString().isEmpty()) {
            return;
        }
        Elements jsoupElements = jsoupDocument.select("a[href]");
        for (Element jsoupElement : jsoupElements) {
            String newPageUrl = jsoupElement.attr("abs:href");
            if (isNotFromThisSite(newPageUrl)) {
                continue;
            }
            IndexedPage newPage = new IndexedPage();
            newPage.setSite(this.page.getSite());
            newPage.setContent(getNewPagePath(newPageUrl));
            newPage.setHttpResponseCode(0);
            newPage.setContent("");
            subPages.add(new IndexedPage());
        }
    }

    private boolean isNotFromThisSite(String pageUrl) {
        return pageUrl.matches(this.page.getSite().getUrl() + pathREGEX);
    }

    private String getNewPagePath(String newPageUrl) {
        return newPageUrl.substring(this.page.getSite().getUrl().length());
    }

}
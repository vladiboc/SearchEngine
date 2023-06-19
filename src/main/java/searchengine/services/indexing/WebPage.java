package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.config.ConnectionHeaders;
import searchengine.model.entities.IndexedPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class WebPage {

    private static final String pathREGEX = "(\\/[0-9A-Za-z_-]+)+";
    private List<IndexedPage> subPages = new ArrayList<>();
    private IndexedPage page;

    public WebPage(IndexedPage page) {
        this.page = page;
        String currentPageUrl = page.getSite().getUrl() + page.getPath();
        try {
            Connection connection = Jsoup.connect(currentPageUrl)
                .userAgent(IndexingServiceImpl.getConnectionHeaders().getUserAgent())
                .referrer(IndexingServiceImpl.getConnectionHeaders().getReferrer());
            Document jsoupDocument = connection.get();
            PageCollector.updateLastHttpRequestTime(page.getSite());
            page.setHttpResponseCode(connection.response().statusCode());
            page.setContent(jsoupDocument.toString().replace("'", "\\'"));
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
            if (!isUrlFromThisSite(newPageUrl)) {
                continue;
            }
            IndexedPage newPage = new IndexedPage();
            newPage.setSite(this.page.getSite());
            newPage.setPath(getPagePath(newPageUrl));
            newPage.setHttpResponseCode(0);
            newPage.setContent("");
            subPages.add(newPage);
        }
    }

    private boolean isUrlFromThisSite(String pageUrl) {
        String regSiteUrl = this.page.getSite().getUrl().replace("/", "\\/");
        boolean isMatch = pageUrl.matches("^" + regSiteUrl + pathREGEX);
        return pageUrl.matches("^" + regSiteUrl + pathREGEX);
    }

    private String getPagePath(String pageUrl) {
        return pageUrl.substring(this.page.getSite().getUrl().length());
    }

}
package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import searchengine.model.entities.IndexedPage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
public class WebPage {

    private List<IndexedPage> subPages = new ArrayList<>();
    private final IndexedPage page;
    private String siteHost;

    public WebPage(IndexedPage page) {
        this.page = page;
        URI siteUri = getUri(page.getSite().getUrl());
        if (siteUri == null) {
            return;
        }
        this.siteHost = siteUri.getHost();
        String currentPageUri = page.getSite().getUrl() + page.getPath();
        try {
            Document jsoupDocument = Jsoup.connect(currentPageUri)
                    .userAgent(IndexingServiceImpl.getConnectionHeaders().getUserAgent())
                    .referrer(IndexingServiceImpl.getConnectionHeaders().getReferrer())
                    .get();
            PageCollector.updateLastHttpRequestTime(page.getSite());
            if (page.getPath().equals("/")) {
                resetSiteHost(jsoupDocument);
            }
            page.setHttpResponseCode(jsoupDocument.connection().response().statusCode());
            page.setContent(jsoupDocument.toString().replace("'", "\\'"));
            if (jsoupDocument.connection().response().statusCode() == HttpStatus.OK.value()) {
                parseSubPages(jsoupDocument);
            }
        } catch (IOException ioException) {
            try {
                HttpStatusException httpException = (HttpStatusException) ioException;
                page.setHttpResponseCode(httpException.getStatusCode());
                page.setContent(httpException.getMessage().replace("'", "\\'"));
            } catch (Exception exception) {
            }
        }
    }

    private void parseSubPages(Document jsoupDocument) {
        if (jsoupDocument == null || jsoupDocument.toString().isEmpty()) {
            return;
        }
        Elements jsoupElements = jsoupDocument.select("a[href]");
        for (Element jsoupElement : jsoupElements) {
            String newPageHref = jsoupElement.attr("abs:href");
            URI newPageUri = getUri(newPageHref);
            if (newPageUri == null) {
                continue;
            }
            String newPageHost = newPageUri.getHost();
            if (newPageHost == null || !newPageHost.equals(siteHost)) {
                continue;
            }
            String newPagePath = newPageUri.getPath().replaceAll("^\\/+", "\\/");
            if (newPagePath.isEmpty() || newPagePath.equals("/")) {
                continue;
            }
            IndexedPage newPage = new IndexedPage();
            newPage.setSite(page.getSite());
            newPage.setPath(newPagePath);
            newPage.setHttpResponseCode(0);
            newPage.setContent("");
            subPages.add(newPage);
        }
    }

    private URI getUri(String string) {
       URI uri = null;
       try {
           uri = new URI(string);
       } catch (URISyntaxException e) {
       }
       return uri;
    }

    private void resetSiteHost(Document jsoupDocument) {
        URL siteUrl = jsoupDocument.connection().response().url();
        siteHost = siteUrl.getHost();
        String stringSiteUrl = siteUrl.toString().replaceAll("\\/$", "");
        page.getSite().setUrl(stringSiteUrl);
    }

}
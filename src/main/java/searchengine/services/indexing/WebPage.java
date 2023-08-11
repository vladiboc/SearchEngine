package searchengine.services.indexing;

import lombok.Getter;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import searchengine.model.entities.IndexedPage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
public class WebPage {

    private final IndexedPage page;
    private final URL pageUrl;
    private final String stringPageUrl;
    private String siteHost;
    private List<IndexedPage> subPages = new ArrayList<>();

    public static synchronized @Nullable URL makeUrlFromString(final String string) {
        try {
            return new URL(string);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String makeStringFromUrl(URL siteUrl) {
        return siteUrl.getProtocol() + "://" + siteUrl.getHost();
    }

    public WebPage(final IndexedPage page, final @Nullable URL pageUrl) {
        this.page = page;
        this.pageUrl = pageUrl;
        this.siteHost = (pageUrl == null) ? null : pageUrl.getHost();
        this.stringPageUrl = page.getSite().getUrl() + page.getPath();
    }

    public WebPage(final IndexedPage page) {
        this(page, makeUrlFromString(page.getSite().getUrl()));
    }

    public void requestAndParseSubPages() {
        if (this.pageUrl == null) {
            return;
        }
        Document jsoupDocument = requestWebPage();
        if (jsoupDocument == null) {
            return;
        }
        if (this.page.getPath().equals("/")) {
            resetSiteUrl(jsoupDocument);
        }
        if (jsoupDocument.connection().response().statusCode() == HttpStatus.OK.value()) {
            parseSubPages(jsoupDocument);
        }
    }

    public void resetSiteUrl(final Document jsoupDocument) {
        final URL responseUrl = jsoupDocument.connection().response().url();
        siteHost = responseUrl.getHost();
        page.getSite().setUrl(makeStringFromUrl(responseUrl));
    }

    public @Nullable Document requestWebPage() {
        try {
            final Document jsoupDocument = Jsoup.connect(this.stringPageUrl)
                    .userAgent(IndexingServiceImpl.getConnectionHeaders().getUserAgent())
                    .referrer(IndexingServiceImpl.getConnectionHeaders().getReferrer())
                    .get();
            this.page.setHttpResponseCode(jsoupDocument.connection().response().statusCode());
            this.page.setContent(jsoupDocument.toString().replace("'", "\\'"));
            return jsoupDocument;
        } catch (IOException ioException) {
            try {
                HttpStatusException httpException = (HttpStatusException) ioException;
                this.page.setHttpResponseCode(httpException.getStatusCode());
                this.page.setContent(httpException.getMessage().replace("'", "\\'"));
                return null;
            } catch (Exception exception) {
                exception.printStackTrace();
                this.page.setContent(exception.getMessage().replace("'", "\\'"));
                return null;
            }
        }
    }

    private void parseSubPages(final @Nullable Document jsoupDocument) {
        if (jsoupDocument == null || jsoupDocument.toString().isEmpty()) {
            return;
        }
        final Elements jsoupElements = jsoupDocument.select("a[href]");
        for (final Element jsoupElement : jsoupElements) {
            final String newPageHref = jsoupElement.attr("abs:href");
            final URL newPageUrl = makeUrlFromString(newPageHref);
            if (newPageUrl == null) {
                continue;
            }
            final String newPageHost = newPageUrl.getHost();
            if (newPageHost == null || !newPageHost.equals(siteHost)) {
                continue;
            }
            final String newPagePath = newPageUrl.getPath().replaceAll("^\\/+", "\\/");
            if (newPagePath.isEmpty() || newPagePath.equals("/")) {
                continue;
            }
            final IndexedPage newPage = new IndexedPage();
            newPage.setSite(page.getSite());
            newPage.setPath(newPagePath);
            newPage.setHttpResponseCode(0);
            newPage.setContent("");
            subPages.add(newPage);
        }
    }

}
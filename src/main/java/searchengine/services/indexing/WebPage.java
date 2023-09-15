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
import searchengine.utils.ConfigPlug;
import searchengine.utils.WebUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class WebPage {
    private final IndexedPage page;
    private final String stringPageUrl;
    private String siteHost;
    private List<IndexedPage> subPages = new ArrayList<>();

    public WebPage(final IndexedPage page) {
        this.page = page;
        URL siteUrl = WebUtils.makeUrlFromString(page.getSite().getUrl());
        Objects.requireNonNull(siteUrl);
        this.siteHost = siteUrl.getHost();
        this.stringPageUrl = page.getSite().getUrl() + page.getPath();
    }

    public void requestAndParseSubPages() {
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
        page.getSite().setUrl(WebUtils.makeStringFromUrl(responseUrl));
    }

    public @Nullable Document requestWebPage() {
        try {
            final Document jsoupDocument = Jsoup.connect(this.stringPageUrl)
                .userAgent(ConfigPlug.getHeadersFromConfig().getUserAgent())
                .referrer(ConfigPlug.getHeadersFromConfig().getReferrer())
                .get();
            this.page.setHttpResponseCode(jsoupDocument.connection().response().statusCode());
            this.page.setContent(jsoupDocument.toString().replace("'", "\\'"));
            return jsoupDocument;
        } catch (HttpStatusException e) {
            this.page.setHttpResponseCode(e.getStatusCode());
            this.page.setContent(e.getMessage().replace("'", "\\'"));
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            this.page.setContent(e.getMessage().replace("'", "\\'"));
            return null;
        }
    }

    private void parseSubPages(final @Nullable Document jsoupDocument) {
        if (jsoupDocument == null || jsoupDocument.toString().isEmpty()) {
            return;
        }
        final Elements jsoupElements = jsoupDocument.select("a[href]");
        for (final Element jsoupElement : jsoupElements) {
            final String newPageHref = jsoupElement.attr("abs:href");
            final URL newPageUrl = WebUtils.makeUrlFromString(newPageHref);
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
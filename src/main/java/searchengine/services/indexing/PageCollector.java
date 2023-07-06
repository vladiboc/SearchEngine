package searchengine.services.indexing;

import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.DbConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.RecursiveTask;

import static java.time.LocalDateTime.now;

public class PageCollector extends RecursiveTask<Boolean> {
    private static DbConnection dbConnection;
    private static volatile HashMap<IndexedSite, Long> siteRequestDelay = new HashMap<>();
    private static volatile HashMap<IndexedSite, Long> lastRequestTime = new HashMap<>();
    private static volatile HashMap<IndexedSite, HashSet<String>> collectedPaths = new HashMap<>();

    private final IndexedPage currentPage;
    private final IndexedSite currentSite;
    private final String currentPath;

    public PageCollector(IndexedPage page, long siteHttpRequestDelay, DbConnection dbConnection) {
        this(page);
        PageCollector.dbConnection = dbConnection;
        PageCollector.siteRequestDelay.put(currentSite, siteHttpRequestDelay);
        PageCollector.lastRequestTime.put(currentSite, System.currentTimeMillis());
        PageCollector.collectedPaths.put(currentSite, new HashSet<>());
    }

    public PageCollector(IndexedPage page) {
        this.currentPage = page;
        this.currentSite = page.getSite();
        this.currentPath = page.getPath();
    }

    public static void updateLastHttpRequestTime(IndexedSite site) {
        synchronized (lastRequestTime) {
            lastRequestTime.put(site, System.currentTimeMillis());
        }
    }

    @Override
    public Boolean compute() {
        if (isCollectedPathsContains(currentPath)) {
            return true;
        }
        addCurrentPathToSitePaths();
        try {
            makePauseForSiteRequestDelay();
        } catch (InterruptedException e) {
            return updateSiteRecordByInterruption();
        }
        WebPage webPage = new WebPage(currentPage);
        saveRequestedPageData();
        if (Thread.currentThread().isInterrupted()) {
            return updateSiteRecordByInterruption();
        }
        ArrayList<PageCollector> taskList = new ArrayList<>();
        for (IndexedPage subPage : webPage.getSubPages()) {
            if (isCollectedPathsContains(subPage.getPath())) {
                continue;
            }
            PageCollector task = new PageCollector(subPage);
            task.fork();
            taskList.add(task);
        }
        for (PageCollector task : taskList) {
            task.join();
        }
        if (Thread.currentThread().isInterrupted()) {
            return updateSiteRecordByInterruption();
        }
        return updateSiteRecordWhenCollected();
    }

    private synchronized boolean isCollectedPathsContains(String path) {
        return collectedPaths.get(currentSite).contains(path);
    }

    private synchronized void addCurrentPathToSitePaths() {
        collectedPaths.get(currentSite).add(currentPath);
    }

    private void makePauseForSiteRequestDelay() throws InterruptedException {
        long nextRequestTime = lastRequestTime.get(currentSite) + siteRequestDelay.get(currentSite);
        while (true) {
            Thread.sleep(siteRequestDelay.get(currentSite));
            if (System.currentTimeMillis() > nextRequestTime) {
                return;
            }
        }
    }

    private boolean updateSiteRecordByInterruption() {
        currentSite.setIndexingStatus(IndexingStatus.FAILED);
        currentSite.setLastError("Индексация остановлена пользователем");
        updateSiteRecord();
        return true;
    }

    private boolean updateSiteRecordWhenCollected() {
        if (currentPath.equals("/")) {
            currentSite.setIndexingStatus(IndexingStatus.INDEXED);
            updateSiteRecord();
        }
        return true;
    }

    private void updateSiteRecord() {
        currentSite.setIndexingStatusTime(now());
        dbConnection.saveSite(currentSite);
    }

    private void saveRequestedPageData() {
        dbConnection.savePage(currentPage);
        currentPage.setContent(null);
        updateSiteRecord();
    }

}
package searchengine.services.indexing;

import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.dbconnectors.DbConnectorIndexing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class PageCollector extends RecursiveTask<Boolean> {
    private static DbConnectorIndexing dbcIndexing;
    private final static HashMap<IndexedSite, Long> siteRequestDelay = new HashMap<>();
    private final static HashMap<IndexedSite, Long> lastRequestTime = new HashMap<>();
    private final static HashMap<IndexedSite, HashSet<String>> collectedPaths = new HashMap<>();

    private final IndexedPage currentPage;
    private final IndexedSite currentSite;
    private final String currentPath;

    public PageCollector(IndexedPage page, long siteHttpRequestDelay, DbConnectorIndexing dbcIndexing) {
        this(page);
        PageCollector.dbcIndexing = dbcIndexing;
        PageCollector.siteRequestDelay.put(currentSite, siteHttpRequestDelay);
        PageCollector.lastRequestTime.put(currentSite, System.currentTimeMillis());
        PageCollector.collectedPaths.put(currentSite, new HashSet<>());
    }

    public PageCollector(IndexedPage page) {
        this.currentPage = page;
        this.currentSite = page.getSite();
        this.currentPath = page.getPath();
    }

    @Override
    public Boolean compute() {
        if (isCollectedPathsContains(currentPath)) {
            return false;
        }
        addCurrentPathToCollectedPaths();
        try {
            makePauseForSiteRequestDelay();
        } catch (InterruptedException e) {
            return updateSiteRecordByInterruption();
        }
        WebPage webPage = new WebPage(currentPage);
        webPage.requestAndParseSubPages();
        updateLastHttpRequestTime(currentPage.getSite());
        dbcIndexing.updateSiteAndPage(currentPage);
        LemmaIndexCollector lemmaIndexCollector = new LemmaIndexCollector(currentPage, dbcIndexing);
        lemmaIndexCollector.collectAndUpdateLemmasAndIndex();
        currentPage.setContent(null);
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
        taskList.forEach(ForkJoinTask::join);
        if (Thread.currentThread().isInterrupted()) {
            return updateSiteRecordByInterruption();
        }
        return updateSiteRecordWhenCollected();
    }

    private synchronized boolean isCollectedPathsContains(String path) {
        return collectedPaths.get(currentSite).contains(path);
    }

    private synchronized void addCurrentPathToCollectedPaths() {
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
        dbcIndexing.updateSite(currentSite);
        return true;
    }

    private void updateLastHttpRequestTime(IndexedSite site) {
        synchronized (lastRequestTime) {
            lastRequestTime.put(site, System.currentTimeMillis());
        }
    }

    private boolean updateSiteRecordWhenCollected() {
        if (currentPath.equals("/")) {
            currentSite.setIndexingStatus(IndexingStatus.INDEXED);
            dbcIndexing.updateSite(currentSite);
        }
        return true;
    }

}
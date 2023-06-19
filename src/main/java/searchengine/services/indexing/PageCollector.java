package searchengine.services.indexing;

import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexedSite;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.DbConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import static java.time.LocalDateTime.now;

public class PageCollector extends RecursiveTask<Boolean> {
    private static DbConnection dbConnection;
    private static volatile List<Thread> collectingThreads = new ArrayList<>();
    private static volatile HashMap<IndexedSite, Long> siteRequestDelay = new HashMap<>();
    private static volatile HashMap<IndexedSite, Long> lastRequestTime = new HashMap<>();
    private static volatile HashMap<IndexedSite, HashSet<String>> collectedPaths = new HashMap<>();

    private final IndexedPage currentPage;
    private final IndexedSite currentSite;
    private final String currentPath;
    private Thread currentThread;

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

    public static int getCollectingThreadsNumber() {
        return collectingThreads.size();
    }

    public static void interruptAlliCollectingThreads() {
        synchronized (collectingThreads) {
            collectingThreads.forEach(thread -> thread.interrupt());
        }
    }

    public static void updateLastHttpRequestTime(IndexedSite site) {
        synchronized (lastRequestTime) {
            lastRequestTime.put(site, System.currentTimeMillis());
        }
    }

    @Override
    public Boolean compute() {
        this.currentThread = Thread.currentThread();
        addCurrentThreadToList();
        if (isCollectedPathsContains(currentPath)) {
            return removeCurrentThreadFromList();
        }
        addCurrentPathToSitePaths();
        try {
            long nextRequestTime = lastRequestTime.get(currentSite) + siteRequestDelay.get(currentSite);
            while (System.currentTimeMillis() < nextRequestTime) {
                Thread.sleep(siteRequestDelay.get(currentSite));
            }
        } catch (InterruptedException e) {
            return exitByInterruption();
        }
        WebPage webPage = new WebPage(currentPage);
        saveRequestedPageData();
        if (currentThread.isInterrupted()) {
            return exitByInterruption();
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
        taskList.forEach(task -> task.join());
        if (currentThread.isInterrupted()) {
            return exitByInterruption();
        }
        return exitWhenCollected();
    }

    private void addCurrentThreadToList() {
        synchronized (collectingThreads) {
            collectingThreads.add(currentThread);
        }
    }

    private boolean removeCurrentThreadFromList() {
        synchronized (collectingThreads) {
            return collectingThreads.remove(currentThread);
        }
    }

    private synchronized boolean isCollectedPathsContains(String path) {
        return collectedPaths.get(currentSite).contains(path);
    }

    private synchronized void addCurrentPathToSitePaths() {
        collectedPaths.get(currentSite).add(currentPath);
    }

    private boolean exitByInterruption() {
        boolean isThreadRemoved = removeCurrentThreadFromList();
        updateSiteRecordByInterruption();
        interruptAlliCollectingThreads();
        return isThreadRemoved;
    }

    private void updateSiteRecordByInterruption() {
        currentSite.setIndexingStatus(IndexingStatus.FAILED);
        currentSite.setLastError("Индексация остановлена пользователем");
        updateSiteRecordTime();
    }

    private boolean exitWhenCollected() {
        boolean isThreadRemoved = removeCurrentThreadFromList();
        if (collectingThreads.size() == 0) {
            updateSiteRecordWhenCollected();
        }
        return isThreadRemoved;
    }

    private void updateSiteRecordWhenCollected() {
        currentSite.setIndexingStatus(IndexingStatus.INDEXED);
        updateSiteRecordTime();
    }

    private void updateSiteRecordTime() {
        currentSite.setIndexingStatusTime(now());
        dbConnection.saveSite(currentSite);
    }

    private void saveRequestedPageData() {
        dbConnection.savePage(currentPage);
        updateSiteRecordTime();
    }

}
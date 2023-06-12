package searchengine.services.indexing;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.dbconnection.DbConnection;
import searchengine.model.entities.IndexedPage;
import searchengine.model.entities.IndexingStatus;
import searchengine.model.repositories.IndexedSitePageRepository;
import searchengine.model.repositories.IndexedSiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveTask;

@Component
@RequiredArgsConstructor
public class PageCollector extends RecursiveTask<Boolean> {

    @Autowired
    private IndexedSiteRepository indexedSiteRepository;
    @Autowired
    private IndexedSitePageRepository indexedSitePageRepository;
    private static volatile List<Thread> collectingThreads = new ArrayList<>();
    private final IndexedPage page;
    private final long siteHttpRequestDelay;

    public static int getCollectingThreadsNumber() {
        return collectingThreads.size();
    }

    public static void interruptAlliCollectingThreads() {
        synchronized (collectingThreads) {
            collectingThreads.forEach(thread -> thread.interrupt());
        }
    }

    @Override
    public Boolean compute() {
        Thread currentThread = Thread.currentThread();
        addThreadToList(currentThread);
        try {
            Thread.sleep(siteHttpRequestDelay);
        } catch (InterruptedException e) {
            return exitByInterruption(currentThread);
        }
        WebPage webPage = new WebPage(page);
        saveRequestedPageData();
        ArrayList<PageCollector> taskList = new ArrayList<>();
        HashSet<String> collectedPaths = (new DbConnection()).getCollectedPaths(page.getSite());
        for (IndexedPage subPage : webPage.getSubPages()) {
            if (collectedPaths.contains(page.getPath())) {
                continue;
            }
            PageCollector task = new PageCollector(subPage, siteHttpRequestDelay);
            task.fork();
            taskList.add(task);
        }
        if (currentThread.isInterrupted()) {
            return exitByInterruption(currentThread);
        }
        taskList.forEach(task -> task.join());
        return removeThreadFromList(currentThread);
    }

    private void addThreadToList(Thread thread) {
        synchronized (collectingThreads) {
            collectingThreads.add(thread);
        }
    }

    private boolean exitByInterruption(Thread thread) {
        boolean isThreadRemoved = removeThreadFromList(thread);
        updateSiteRecordByInterruption();
        interruptAlliCollectingThreads();
        return isThreadRemoved;
    }

    private boolean removeThreadFromList(Thread thread) {
        synchronized (collectingThreads) {
            return collectingThreads.remove(thread);
        }
    }

    private void updateSiteRecordByInterruption() {
        page.getSite().setIndexingStatus(IndexingStatus.FAILED);
        page.getSite().setLastError("Индексация остановлена пользователем");
        updateSiteRecordTime();
    }

    private void updateSiteRecordTime() {
        page.getSite().setIndexingStatusTime(LocalDateTime.now());
        indexedSiteRepository.save(page.getSite());
    }

    private void saveRequestedPageData() {
        indexedSitePageRepository.save(page);
        updateSiteRecordTime();
    }

}
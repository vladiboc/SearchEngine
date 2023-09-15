package searchengine.services.search;

import lombok.Data;
import searchengine.model.entities.SiteLemma;

import java.util.ArrayList;
import java.util.List;

@Data
public class LemmaData {
    private String lemma;
    private int frequency;
    private List<Integer> ids = new ArrayList<>();
}

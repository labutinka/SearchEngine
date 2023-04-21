package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@Service
public class LemmaService {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaService(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public HashMap<String, Integer> collectLemmas (String text){
        String[] words = arrayContainsRussianWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words){
            if (word.isBlank()) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }

        }
        return lemmas;
    }

    public String clearContent(PageEntity page){
        StringBuilder clearedText = new StringBuilder();
        clearedText.append(Jsoup.clean(page.getContent(), Safelist.relaxed()));
        return clearedText.toString();
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}

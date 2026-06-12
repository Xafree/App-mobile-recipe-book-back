package fr.gymgod.etl.gateway.scraping;

import fr.gymgod.etl.domain.model.AdditiveData;
import fr.gymgod.etl.domain.port.AdditiveSourcePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class QueChoisirGateway implements AdditiveSourcePort {

    private static final String QC_URL = "https://www.quechoisir.org/comparatif-additifs-alimentaires-n56877/";

    @Override
    public List<AdditiveData> fetchAdditives() {
        log.info("Scraping additives from QueChoisir: {}", QC_URL);
        List<AdditiveData> additives = new ArrayList<>();

        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(QC_URL)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(30000)
                    .get();

            org.jsoup.select.Elements articles = doc.select("article.qc-product-card");
            log.info("Found {} additive cards", articles.size());

            for (org.jsoup.nodes.Element article : articles) {
                try {
                    String dataName = article.attr("data-name").trim();
                    if (dataName.isEmpty())
                        continue;

                    String code;
                    String name;
                    int firstSpace = dataName.indexOf(' ');
                    if (firstSpace > 0) {
                        code = dataName.substring(0, firstSpace).trim();
                        name = dataName.substring(firstSpace + 1).trim();
                    } else {
                        code = dataName;
                        name = dataName;
                    }

                    int dangerLevel = 0;
                    org.jsoup.nodes.Element pictoImg = article
                            .selectFirst(".qc-product-card_appreciation img[src*='picto-']");
                    if (pictoImg != null) { 
                        String src = pictoImg.attr("src");
                        if (src.contains("picto-1"))
                            dangerLevel = 1;
                        else if (src.contains("picto-2"))
                            dangerLevel = 2;
                        else if (src.contains("picto-3"))
                            dangerLevel = 3;
                        else if (src.contains("picto-4"))
                            dangerLevel = 4;
                    }

                    AdditiveData additive = new AdditiveData();
                    additive.setCode(code);
                    additive.setName(name);
                    additive.setDangerLevel(dangerLevel);

                    additives.add(additive);

                } catch (Exception e) {
                    log.error("Error processing additive card", e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scrape QueChoisir", e);
        }

        return additives;
    }
}

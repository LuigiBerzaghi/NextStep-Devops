package com.softcode.nextstep.service;

import com.softcode.nextstep.api.dto.profession.ProfessionSuggestionItem;
import com.softcode.nextstep.api.dto.profession.ProfessionSuggestionResponse;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfessionService {

    private static final List<ProfessionSuggestionItem> CATALOG = List.of(
            new ProfessionSuggestionItem(
                    "prof-1",
                    "Product Designer",
                    "Design",
                    "92%",
                    "Profissional que une UX/UI a visao estrategica de produto"),
            new ProfessionSuggestionItem(
                    "prof-2",
                    "Full Stack Developer",
                    "Tecnologia",
                    "88%",
                    "Desenvolvedor completo, frontend e backend"),
            new ProfessionSuggestionItem(
                    "prof-3",
                    "UX Researcher",
                    "Design",
                    "85%",
                    "Especialista em pesquisa com usuarios"),
            new ProfessionSuggestionItem(
                    "prof-4",
                    "Data Scientist",
                    "Dados",
                    "78%",
                    "Analista focado em modelos de machine learning"),
            new ProfessionSuggestionItem(
                    "prof-5",
                    "Cloud Engineer",
                    "Tecnologia",
                    "81%",
                    "Profissional de infraestrutura e automacao em cloud"));

    public ProfessionSuggestionResponse findSuggestions(String search) {
        List<ProfessionSuggestionItem> items = CATALOG;
        if (StringUtils.hasText(search)) {
            String normalized = search.toLowerCase(Locale.ROOT);
            items = CATALOG.stream()
                    .filter(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized)
                            || item.category().toLowerCase(Locale.ROOT).contains(normalized))
                    .collect(Collectors.toList());
        }
        return new ProfessionSuggestionResponse(items);
    }
}


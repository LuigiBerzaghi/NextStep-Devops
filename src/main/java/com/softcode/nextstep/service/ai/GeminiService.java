package com.softcode.nextstep.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.common.InsightDto;
import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.journey.JourneyGenerationRequest;
import com.softcode.nextstep.api.dto.resume.ResumeSummaryDto;
import com.softcode.nextstep.config.GeminiProperties;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.service.RateLimitService;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private static final Duration GEMINI_WINDOW = Duration.ofMinutes(1);
    private static final int GEMINI_LIMIT = 15;
    private static final String RESUME_PROMPT = """
            Voce e um especialista em carreira. Responda APENAS com JSON valido (sem Markdown, listas ou comentarios).
            Seja direto e objetivo. Formato esperado:
            {
              "experienceLevel": "Junior/Pleno/Senior",
              "currentJob": "Cargo atual ou desejado",
              "yearsOfExperience": 4,
              "currentSkills": [
                 {"name": "Skill", "level": "Basico/Intermediario/Avancado", "progress": 0-100}
              ],
              "gaps": ["Lacunas objetivas..."],
              "suggestedCareers": [
                 {"title": "Cargo", "match": "88%", "reason": "Motivo textual"}
              ]
            }
            Certifique-se de que o JSON seja valido. O texto do curriculo vem a seguir.
            Nao inclua texto em Markdown ou comentarios fora do JSON.
            """;
    private static final String JOURNEY_PROMPT = """
            Voce e um mentor de carreira. A partir dos dados fornecidos (skills atuais, lacunas e cargo desejado),
            responda APENAS com um JSON objetivo:
            {
              "estimatedTime": "Ex.: 16 semanas",
              "steps": [
                 {"order":1,"title":"","objective":"","resources":"","platforms":["Coursera"],"estimatedTime":""}
              ],
              "insights": [
                 {"type":"skill|trend|certification","icon":"lightbulb","text":"Insight objetivo"}
              ]
            }
            Use apenas nomes validos da biblioteca Lucide Icons para o campo icon (ex: lightbulb, trending-up, award, target, sparkles).
            Nao escreva nada fora desse JSON.
            """;
    private static final String CHAT_PROMPT = """
            Voce e o Mentor AI da NextStep, um especialista em carreira. Sua resposta DEVE seguir TODAS as regras abaixo:
        1.  NAO use Markdown. Nao inclua '*', '#', '-', ou qualquer outro caractere de formatacao.
        2.  Responda APENAS com texto corrido, usando paragrafos curtos para separar as ideias.
        3.  Seja direto e conciso, mantendo a resposta curta.
        4.  Ofereca no maximo 3 orientacoes praticas por resposta.
        5.  Use um tom de mentor: empatico, claro e encorajador.
        O objetivo e uma resposta limpa e facil de ler.
            """;

    private static final Set<String> LUCIDE_ICONS = Set.of(
            "lightbulb",
            "trendingUp",
            "award",
            "target",
            "sparkles",
            "zap",
            "bookOpen",
            "layers",
            "globe2",
            "compass",
            "brain",
            "flag",
            "star",
            "shieldCheck",
            "rocket",
            "activity");

    private final RateLimitService rateLimitService;
    private final GeminiProperties geminiProperties;
    private final Client geminiClient;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ObjectMapper objectMapper;

    public ResumeSummaryDto analyzeResume(User user, String fileName, byte[] content) {
        enforceAiLimit(user);
        String resumeText = resumeTextExtractor.extractText(content, fileName);
        String response = callGemini(RESUME_PROMPT, buildResumeContext(user, fileName, resumeText));
        ResumeSummaryPayload payload = parseResponse(response, ResumeSummaryPayload.class);
        return payload.toDto();
    }

    public JourneyPlan generateJourneyPlan(User user, JourneyGenerationRequest request) {
        enforceAiLimit(user);
        String context = """
                Usuario: %s
                Cargo desejado: %s
                Habilidades atuais: %s
                Lacunas identificadas: %s
                """
                .formatted(
                        user.getName() == null ? user.getEmail() : user.getName(),
                        request.desiredJob(),
                        String.join(", ", request.currentSkills()),
                        String.join(", ", request.gaps()));
        String response = callGemini(JOURNEY_PROMPT, context);
        JourneyPlanPayload payload = parseResponse(response, JourneyPlanPayload.class);
        JourneyPlan plan = payload.toPlan();
        return new JourneyPlan(plan.estimatedTime(), plan.steps(), sanitizeInsights(plan.insights()));
    }

    public String answerChat(User user, String prompt) {
        enforceAiLimit(user);
        String context = """
                Usuario: %s (%s)
                Mensagem: %s
                """
                .formatted(
                        user.getName() == null ? user.getEmail() : user.getName(),
                        user.getCurrentJob() == null ? "sem cargo definido" : user.getCurrentJob(),
                        prompt);
        return cleanPlainText(callGemini(CHAT_PROMPT, context));
    }

    private void enforceAiLimit(User user) {
        rateLimitService.validateWithinLimit("gemini:" + user.getId(), GEMINI_LIMIT, GEMINI_WINDOW);
    }

    private String buildResumeContext(User user, String fileName, String resumeText) {
        return """
                Usuario: %s
                Arquivo: %s

                Texto do curriculo:
                %s
                """
                .formatted(user.getName() == null ? user.getEmail() : user.getName(), fileName, resumeText);
    }

    public record JourneyPlan(String estimatedTime, List<JourneyStepPlan> steps, List<InsightDto> insights) {}

    public record JourneyStepPlan(
            int order, String title, String objective, String resources, List<String> platforms, String estimatedTime) {}

    private record ResumeSummaryPayload(
            String experienceLevel,
            String currentJob,
            int yearsOfExperience,
            List<SkillDto> currentSkills,
            List<String> gaps,
            List<CareerSuggestionDto> suggestedCareers) {

        ResumeSummaryDto toDto() {
            return new ResumeSummaryDto(
                    currentSkills == null ? List.of() : currentSkills,
                    experienceLevel == null ? "Desconhecido" : experienceLevel,
                    currentJob,
                    yearsOfExperience,
                    gaps == null ? List.of() : gaps,
                    suggestedCareers == null ? List.of() : suggestedCareers);
        }
    }

    private record JourneyPlanPayload(
            String estimatedTime, List<JourneyStepPayload> steps, List<InsightDto> insights) {

        JourneyPlan toPlan() {
            List<JourneyStepPlan> convertedSteps = (steps == null ? List.<JourneyStepPlan>of()
                    : steps.stream().map(JourneyStepPayload::toStep).toList());
            return new JourneyPlan(
                    estimatedTime == null ? "12 semanas" : estimatedTime,
                    convertedSteps,
                    insights == null ? List.of() : insights);
        }
    }

    private record JourneyStepPayload(
            int order, String title, String objective, String resources, List<String> platforms, String estimatedTime) {
        JourneyStepPlan toStep() {
            List<String> safePlatforms = platforms == null ? List.of() : platforms;
            return new JourneyStepPlan(order, title, objective, resources, safePlatforms, estimatedTime);
        }
    }

    private String callGemini(String systemPrompt, String userContext) {
        GenerateContentResponse response = geminiClient.models.generateContent(
                geminiProperties.getModel(), systemPrompt + "\n\n" + userContext, null);
        String text = response.text();
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Resposta vazia do Gemini");
        }
        return text.trim();
    }

    private List<InsightDto> sanitizeInsights(List<InsightDto> insights) {
        if (insights == null) {
            return List.of();
        }
        return insights.stream()
                .map(insight -> new InsightDto(
                        insight.type(), sanitizeIcon(insight.icon()), insight.text()))
                .toList();
    }

    private String sanitizeIcon(String icon) {
        if (!StringUtils.hasText(icon)) {
            return "sparkles";
        }
        String normalized = toCamelCase(icon);
        if (!LUCIDE_ICONS.contains(normalized)) {
            return "sparkles";
        }
        return normalized;
    }

    private String toCamelCase(String value) {
        String cleaned = value.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
        if (!StringUtils.hasText(cleaned)) {
            return "sparkles";
        }
        String[] parts = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder(parts[0].substring(0, 1).toLowerCase(Locale.ROOT)
                + parts[0].substring(1));
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1));
        }
        return builder.toString();
    }

    private <T> T parseResponse(String content, Class<T> type) {
        try {
            String sanitized = sanitizeJson(content);
            return objectMapper.readValue(sanitized, type);
        } catch (Exception ex) {
            String snippet = content == null ? "null" : content.substring(0, Math.min(content.length(), 512));
            log.error("Falha ao converter resposta do Gemini. Conteudo recebido: {}", snippet, ex);
            throw new IllegalStateException("Falha ao converter resposta do Gemini", ex);
        }
    }

    private String sanitizeJson(String response) {
        if (response == null) {
            return "";
        }
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            int fence = trimmed.lastIndexOf("```");
            trimmed = trimmed.substring(0, fence);
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end >= start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return trimmed.trim();
    }

    private String cleanPlainText(String response) {
        if (!StringUtils.hasText(response)) {
            return response;
        }
        String cleaned = response
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\\t", "    ")
                .replace("\\r", "\n");
        // Remove duplicated empty lines caused by conversions
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        cleaned = cleaned.replace("\n", " ");
        cleaned = cleaned.replaceAll(" {2,}", " ");
        return cleaned.trim();
    }
}

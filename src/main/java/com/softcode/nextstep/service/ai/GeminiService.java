package com.softcode.nextstep.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponse;
import com.softcode.nextstep.api.dto.common.CareerSuggestionDto;
import com.softcode.nextstep.api.dto.common.InsightDto;
import com.softcode.nextstep.api.dto.common.SkillDto;
import com.softcode.nextstep.api.dto.resume.ResumeSummaryDto;
import com.softcode.nextstep.config.GeminiProperties;
import com.softcode.nextstep.domain.user.User;
import com.softcode.nextstep.service.RateLimitService;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
            Para as plataformas, pode considerar também cursos de graduação em instituições de renome, caso necessário. Cite apenas a abreviação da instituição (ex: "Universidade de São Paulo" : "USP").
            Caso haja a a necessidade de sugerir algum curso de graduação, coloque ele na primeira etapa do plano.
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
        3.  Mantenha a resposta com, no maximo, 600 caracteres (aproximadamente 3 paragrafos curtos). Em hipótese alguma ultrapasse 600 caracteres.
        4.  Seja direto e conciso e ofereca no maximo 3 orientacoes praticas.
        5.  Use um tom de mentor: empatico, claro e encorajador.
        O objetivo e uma resposta limpa, curta e facil de ler.
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
        if (!geminiProperties.isEnabled()) {
            log.info("Gemini desabilitado via configuracao - aplicando fallback da analise de curriculo.");
            return fallbackResumeSummary(user);
        }
        enforceAiLimit(user);
        String resumeText = resumeTextExtractor.extractText(content, fileName);
        try {
            String response = callGemini(RESUME_PROMPT, buildResumeContext(user, fileName, resumeText));
            ResumeSummaryPayload payload = parseResponse(response, ResumeSummaryPayload.class);
            return payload.toDto();
        } catch (ApiException | IllegalStateException ex) {
            log.warn("Falha ao consultar Gemini para analise de curriculo. Aplicando fallback.", ex);
            return fallbackResumeSummary(user);
        }
    }

    public JourneyPlan generateJourneyPlan(User user, String desiredJob, List<SkillDto> currentSkills, List<String> gaps) {
        if (!geminiProperties.isEnabled()) {
            log.info("Gemini desabilitado via configuracao - retornando jornada padrao.");
            return fallbackJourneyPlan(user, desiredJob);
        }
        enforceAiLimit(user);
        List<SkillDto> safeSkills = currentSkills == null ? List.of() : currentSkills;
        List<String> safeGaps = sanitizeTextList(gaps);
        String gapSummary = safeGaps.isEmpty() ? "Nao informado" : String.join(", ", safeGaps);
        String context = """
                Usuario: %s
                Cargo desejado: %s
                Habilidades atuais: %s
                Lacunas identificadas: %s
                """
                .formatted(
                        resolveUserName(user),
                        desiredJob,
                        formatSkillsForPrompt(safeSkills),
                        gapSummary);
        try {
            String response = callGemini(JOURNEY_PROMPT, context);
            JourneyPlanPayload payload = parseResponse(response, JourneyPlanPayload.class);
            JourneyPlan plan = payload.toPlan();
            return new JourneyPlan(plan.estimatedTime(), plan.steps(), sanitizeInsights(plan.insights()));
        } catch (ApiException | IllegalStateException ex) {
            log.warn("Falha ao consultar Gemini para jornada. Aplicando fallback.", ex);
            return fallbackJourneyPlan(user, desiredJob);
        }
    }

    public String answerChat(User user, String prompt) {
        String response;
        if (!geminiProperties.isEnabled()) {
            log.info("Gemini desabilitado via configuracao - retornando resposta padrao no chat.");
            response = fallbackChatAnswer(prompt);
        } else {
            enforceAiLimit(user);
            String context = """
                    Usuario: %s (%s)
                    Mensagem: %s
                    """
                    .formatted(
                            resolveUserName(user),
                            user.getCurrentJob() == null ? "sem cargo definido" : user.getCurrentJob(),
                            prompt);
            try {
                response = callGemini(CHAT_PROMPT, context);
            } catch (ApiException | IllegalStateException ex) {
                log.warn("Falha ao consultar Gemini no chat. Aplicando fallback.", ex);
                response = fallbackChatAnswer(prompt);
            }
        }
        return cleanPlainText(response);
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
                .formatted(resolveUserName(user), fileName, resumeText);
    }

    private String resolveUserName(User user) {
        return user.getName() == null ? user.getEmail() : user.getName();
    }

    private String formatSkillsForPrompt(List<SkillDto> skills) {
        if (skills == null || skills.isEmpty()) {
            return "Nao informado";
        }
        return skills.stream()
                .map(this::formatSkill)
                .collect(Collectors.joining(", "));
    }

    private String formatSkill(SkillDto skill) {
        if (skill == null) {
            return "Habilidade";
        }
        String name = StringUtils.hasText(skill.name()) ? skill.name().trim() : "Habilidade";
        String level = StringUtils.hasText(skill.level()) ? " - " + skill.level().trim() : "";
        String progress = skill.progress() == null ? "" : " (" + skill.progress() + "%)";
        return name + level + progress;
    }

    private List<String> sanitizeTextList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
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

    private ResumeSummaryDto fallbackResumeSummary(User user) {
        String currentJob = user.getCurrentJob() == null ? "Profissional de tecnologia" : user.getCurrentJob();
        List<SkillDto> skills = List.of(
                new SkillDto("Comunicacao", "Intermediario", 70),
                new SkillDto("Planejamento", "Intermediario", 65),
                new SkillDto("Aprendizado continuo", "Avancado", 80));
        List<String> gaps = List.of("Certificacao cloud", "Ingles avancado");
        List<CareerSuggestionDto> suggestions = List.of(
                new CareerSuggestionDto("Product Owner", "82%", "Voce domina visao de produto e pode liderar discovery."),
                new CareerSuggestionDto("Tech Lead", "78%", "Ha historico tecnico suficiente para orientar uma squad."),
                new CareerSuggestionDto("Arquiteto de Solucoes", "73%", "Reforce conhecimentos em integracoes e seguranca."));
        return new ResumeSummaryDto(skills, "Pleno", currentJob, 5, gaps, suggestions);
    }

    private JourneyPlan fallbackJourneyPlan(User user, String desiredJob) {
        List<JourneyStepPlan> steps = List.of(
                new JourneyStepPlan(
                        1,
                        "Mapear cenario atual",
                        "Liste entregas recentes e como elas aproximam do cargo %s."
                                .formatted(desiredJob),
                        "Checklist NextStep + feedback rapido do gestor",
                        List.of("NextStep Academy"),
                        "1 semana"),
                new JourneyStepPlan(
                        2,
                        "Fortalecer competencias tecnicas",
                        "Estude arquitetura distribuida e fundamentos de cloud.",
                        "Trilhas Azure/AWS e estudos de caso NextStep",
                        List.of("Azure Learn", "Coursera"),
                        "4 semanas"),
                new JourneyStepPlan(
                        3,
                        "Desenvolver plano de impacto",
                        "Monte roteiro de 60 dias mostrando ganhos esperados e indicadores.",
                        "Template NextStep + sessoes de mentoria",
                        List.of("Notion", "Miro"),
                        "3 semanas"));
        List<InsightDto> insights = List.of(
                new InsightDto("skill", "lightbulb", "Priorize fundamentos de arquitetura e mensuracao de impacto."),
                new InsightDto("trend", "trendingUp", "Empresas valorizam lideres com fluencia em cloud e dados."),
                new InsightDto("certification", "award", "Uma certificacao Azure ou AWS diferencia seu perfil."));
        return new JourneyPlan("8-12 semanas", steps, insights);
    }

    private String fallbackChatAnswer(String prompt) {
        String excerpt = prompt == null ? "" : (prompt.length() > 280 ? prompt.substring(0, 280) + "..." : prompt);
        return """
                Nosso assistente de IA esta indisponivel no momento, mas aqui vai uma orientacao rapida com base no seu texto:

                %s

                Divida seu plano em acoes curtas, registre aprendizados e compartilhe com o time. Assim que o motor de IA voltar, voce podera aprofundar essas sugestoes.
                """
                .formatted(excerpt);
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
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .replace("\r", " ")
                .replace("\\r\\n", " ")
                .replace("\\n", " ")
                .replace("\\t", " ");
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.length() <= 600) {
            return cleaned;
        }
        String truncated = cleaned.substring(0, 600);
        return truncated + "...";
    }
}

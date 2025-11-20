# NextStep
> Plataforma backend para requalificação profissional que conecta a autenticação do Firebase, análises de currículo via IA Gemini e geração de jornadas personalizadas para pessoas em transição de carreira.

# Descrição Geral


## 🌍 Global Solution 2025 - O Futuro do Trabalho

**NextStep** foi desenvolvido para o desafio **Global Solution 2025 da FIAP**, tema: **"Repensando Hoje as Profissões do Amanhã"**.

### 🎯 Alinhamento com o Desafio

> *"Propor soluções que usem inovação e tecnologia para melhorar a vida das pessoas no trabalho, preparar empresas para os novos tempos e criar oportunidades mais justas, inclusivas e sustentáveis."*  
> — **Global Solution FIAP 2025**

**NextStep é a Sugestão #1 da FIAP:** *Plataformas de Upskilling e Reskilling Baseadas em IA* ✅

### 📊 Contexto e Dados de Mercado

De acordo com a **ONU**, **OIT** e **Fórum Econômico Mundial**:

- 📈 **170 milhões de empregos** serão criados entre 2025-2030
- 🔄 **23% das profissões** vão se transformar radicalmente até 2027
- 🤖 **40% das tarefas humanas** podem ser automatizadas nos próximos 5 anos
- ⚡ **60% em 10 anos** - automação em escala acelerada
- 🎓 **Milhões de profissionais** precisarão se requalificar até 2030

**O desafio:** *Como equilibrar eficiência tecnológica com o valor humano?*  
**Nossa resposta:** *IA personaliza, mas VOCÊ decide seu caminho.*

### 🎯 Objetivos de Desenvolvimento Sustentável (ODS)

NextStep contribui diretamente com 4 ODS destacados pela Global Solution:

| ODS | Descrição | Como NextStep Contribui |
|-----|-----------|-------------------------|
| **🎓 ODS 4** | Educação de Qualidade | Acesso democratizado a trilhas de aprendizado personalizadas |
| **💼 ODS 8** | Trabalho Decente e Crescimento Econômico | Requalificação profissional para empregos dignos |
| **🏭 ODS 9** | Indústria, Inovação e Infraestrutura | Uso de IA e tecnologias emergentes |
| **⚖️ ODS 10** | Redução das Desigualdades | Plataforma gratuita, inclusiva e acessível |

---

## 📖 Sobre o Projeto

**NextStep** é uma plataforma inovadora que utiliza **IA (Google Gemini)** para democratizar a requalificação profissional. O sistema analisa currículos, identifica lacunas de conhecimento e gera **jornadas personalizadas** com recursos curados de plataformas como Coursera, Udemy, YouTube e outras.

### 🔥 Por que NextStep?

**O trabalho está mudando. E você pode ajudar a criar o que vem pela frente.**

> *Não fique para trás. **Dê o próximo passo.***  
> Sua próxima carreira começa hoje, com uma jornada personalizada criada por IA.

**NextStep** não é apenas uma plataforma — é o seu parceiro na maior transformação profissional da história. Enquanto o mundo se prepara para 170 milhões de novos empregos e a extinção de milhares de outros, a pergunta não é **se** você vai se requalificar, mas **quando** e **como**.

**A resposta? Agora. Com NextStep.**

#### 🎯 Problema que Resolve
- 🌀 **Profissionais perdidos** em transições de carreira
- 🔍 **Dificuldade em identificar gaps** de conhecimento
- 📚 **Sobrecarga de informação** - qual curso fazer?
- 🛤️ **Falta de trilhas personalizadas** e estruturadas
- ⏰ **Urgência de requalificação** em um mercado em transformação
- 💸 **Barreiras financeiras** para cursos de qualidade

#### 💡 Nossa Solução
- 🤖 **Análise de currículo com IA** (Google Gemini)
- 🎯 **Jornadas personalizadas** baseadas no perfil e objetivo profissional
- 📚 **Curadoria inteligente** de recursos externos gratuitos e pagos
- 💬 **Chatbot assistente** para dúvidas e motivação
- 📊 **Dashboard visual** de evolução e progresso
- 🌐 **Modelo agregador** - conectamos você ao melhor conteúdo do mercado

#### 🌟 Diferencial: Tecnologia + Lado Humano
- ✅ IA analisa e recomenda, mas **você mantém o controle** da sua jornada
- ✅ Foco em **habilidades humanas**: criatividade, empatia, pensamento crítico
- ✅ **Aprender e reaprender**: o novo superpoder da era digital

---


## Arquitetura & Tecnologias
### Visão em camadas
- `api/controller`: REST Controllers expõem endpoints focados em autenticação, dashboard, jornada, profissão, perfil, chat e currículo.
- `api/dto`: records Java que tipam todas as requisições e respostas, concentrando regras de validação (`jakarta.validation`).
- `service`: regras de negócio. Destaques: `GeminiService` (integração IA + rate limit), `JourneyService`, `ResumeService`, `ProfileService`, `ChatService`, etc.
- `domain` + `repository`: entidades JPA (`User`, `Journey`, `JourneyStep`, `ResumeAnalysis`, `ChatMessage`) e interfaces Spring Data preparadas para Azure SQL (SQL Server).
- `security`: filtro `FirebaseAuthenticationFilter` valida JWTs do Firebase, aplica rate limit (`RateLimitService`) e injeta o usuário em `AuthenticatedUserContext`.
- `config`: configurações de cache (Caffeine), internacionalização, RabbitMQ, Gemini Client e MessageSource.
- `messaging`: `NotificationProducer` publica eventos no RabbitMQ, `NotificationListener` registra consumo assíncrono.

### Principais tecnologias
- **Linguagem/Runtime**: Java 17, Maven Wrapper.
- **Framework**: Spring Boot (Web, Security, Validation, Data JPA, Cache, Actuator), Lombok.
- **Banco**: Azure SQL Database / SQL Server (driver `mssql-jdbc`), com dependência H2 apenas para smoke tests locais.
- **IA**: SDK oficial `google-genai` (Gemini 2.5 Flash) + Apache Tika para extrair texto de currículos.
- **Mensageria**: RabbitMQ (AMQP), com conversores Jackson.
- **Outros**: Micrometer Tracing, Spring REST Docs (configurado no `pom`), Testcontainers (RabbitMQ + SQL Server), Caffeine Cache, internacionalização (`messages*.properties`).
- **Documentação auxiliar**: `API-SPEC.md` detalha fluxos de autenticação e endpoints.

### Fluxos adicionais
- **Autenticação**: front-end obtém token Firebase (qualquer projeto), envia como `Authorization: Bearer {jwt}`. O backend decodifica o payload para localizar ou criar o usuário.
- **Rate limiting**: 100 chamadas/min por usuário autenticado; 15 execuções/min para interações com Gemini.
- **Cache**: `DashboardService#getDashboard` usa Caffeine por 5 minutos por usuário.
- **Mensageria**: geração de jornada e análise de currículo disparam eventos (`journey.generated`, `resume.analysis.completed`).

## Funcionalidades Principais
- Cadastro/atualização de perfil com dados trazidos do Firebase (`AuthController`, `ProfileController`).
- Upload e análise de currículos (PDF/DOC/DOCX), com extração de skills, lacunas e carreiras sugeridas usando Gemini.
- Geração e gestão de jornadas personalizadas com passos ordenados, insights e acompanhamento de progresso.
- Dashboard com próximos passos, skills e tendências consolidadas.
- Chat mentorado por IA para tirar dúvidas rápidas (`ChatService`).
- Sugestão de profissões alvo a partir de um catálogo local (mock).
- Histórico de jornadas concluídas, exclusão de conta e limpeza de dados associados.

## API
- **Base URL local**: `http://localhost:8080`
- **Autenticação**: todos os endpoints `/api/**` (exceto `/api/auth/**` e `/api/public/**`) exigem header `Authorization: Bearer {firebase-jwt}`.

### Autenticação & Perfil
| Método | Rota | Descrição | Corpo (req) | Resposta | Status |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/verify` | Decodifica o JWT, cria/busca usuário e indica se há jornada ativa. | — | `AuthVerifyResponse` | 200 |
| POST | `/api/auth/complete-profile` | Atualiza nome e cargo atual após o cadastro no Firebase. | `CompleteProfileRequest` | `CompleteProfileResponse` | 200 |
| GET | `/api/profile` | Retorna dados e estatísticas do usuário autenticado. | — | `ProfileResponse` | 200 |
| PUT | `/api/profile` | Atualiza nome, email e cargo. | `ProfileUpdateRequest` | `ProfileResponse` | 200 |
| DELETE | `/api/profile` | Remove usuário e dados agregados. | — | `DeleteProfileResponse` | 200 |

### Currículo & Dashboard
| Método | Rota | Descrição | Corpo | Resposta | Status |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/resume/upload` | Upload multipart (`file`) até 5 MB (PDF/DOC/DOCX) para análise via Gemini. | `MultipartFile` | `ResumeAnalysisResponse` | 200 |
| GET | `/api/resume/analysis/{userId}` | Retorna a última análise do usuário (valida proprietário). | — | `ResumeAnalysisResponse` | 200/403 |
| GET | `/api/dashboard` | Consolida próximos passos, skills e tendências. Cacheado por usuário. | — | `DashboardResponse` | 200 |

### Jornadas
| Método | Rota | Descrição | Corpo | Resposta | Status |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/journeys/generate` | Cria nova jornada usando Gemini a partir do cargo desejado e da ultima analise de curriculo. | `JourneyGenerationRequest` | `JourneyResponse` | 201 |
| GET | `/api/journeys/active` | Recupera a jornada ativa do usuário. | — | `JourneyResponse` | 200 |
| PATCH | `/api/journeys/steps/{stepId}/progress` | Atualiza progresso de um passo (0–100). | `JourneyProgressUpdateRequest` | `JourneyStepResponse` | 200 |
| GET | `/api/journeys/history?page=&size=` | Paginado das jornadas concluídas. | — | `JourneyHistoryResponse` | 200 |

### Chat & Profissões
| Método | Rota | Descrição | Corpo | Resposta | Status |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/chat/send` | Salva mensagem do usuário, chama Gemini (prompt mentor) e retorna resposta. | `ChatMessageRequest` | `ChatMessageResponse` | 200 |
| GET | `/api/chat/history?conversationId=&page=&size=` | Histórico paginado da conversa ordenado cronologicamente. | — | `ChatHistoryResponse` | 200 |
| GET | `/api/professions/suggested?search=` | Lista catálogo local de profissões filtrado por texto. | — | `ProfessionSuggestionResponse` | 200 |

### Erros comuns
- respostas seguem `ErrorResponse` (`error`, `message`, `details`), internacionalizada via `messages*.properties`.
- códigos relevantes: `401 UNAUTHORIZED` (token ausente/invalidado), `429 TOO MANY REQUESTS` (rate limit), `404 NOT_FOUND` (jornadas/análises inexistentes), `400 BAD_REQUEST` (validações).

## Modelagem de Dados
- **User** (`users`): chave primária UUID, `firebaseUid`, email único, nome, cargo atual, status (`ACTIVE/DELETED`).
- **ResumeAnalysis** (`resume_analysis`): relacionamento N:1 com `User`; guarda JSON de skills, lacunas e carreiras sugeridas, nível de experiência e timestamp da análise.
- **Journey** (`journeys`): pertence a um usuário, status (`ACTIVE/COMPLETED/ARCHIVED`), progresso geral, cargo desejado, insights (JSON) e lista de **JourneyStep**.
- **JourneyStep** (`journey_steps`): ordem, título, objetivo, recursos, plataformas suportadas, tempo estimado, progresso e status (`PENDING/IN_PROGRESS/COMPLETED`).
- **ChatMessage** (`chat_messages`): histórico do chat mentorado (usuário, conversa, papel `USER/AI`, timestamp).
- A persistência é realizada via Spring Data JPA; `BaseEntity` adiciona `createdAt/updatedAt` automáticos.

## Pré-requisitos
- Java 17 (JDK).
- Maven 3.9+ (ou `./mvnw`).
- Azure SQL Database / SQL Server acessível (ajuste `spring.datasource.*`). Para desenvolvimento local pode-se habilitar Testcontainers SQL Server ou apontar para H2.
- RabbitMQ rodando localmente (porta 5672) para envio/consumo de notificações.
- Conta no Google AI Studio para uma API key Gemini (`gemini.api-key`).
- Projeto Firebase para gerar tokens JWT (pode usar o projeto demonstrativo configurado nos HTMLs).

## Como Rodar o Projeto Localmente
```bash
# 1. Clonar o repositório
git clone <url-do-repo>
cd nextstep

# 2. Configurar credenciais sensíveis (recomenda-se variáveis ou application-local.properties)
cp src/main/resources/application.properties .env # ajuste Azure SQL, RabbitMQ, Gemini etc.

# Alternativamente exporte (PowerShell):
$env:SPRING_DATASOURCE_URL="jdbc:sqlserver://host:1433;database=db;user=user@host;password=Senha!;encrypt=true;trustServerCertificate=false;loginTimeout=30;"
$env:GEMINI_API_KEY="sua-chave"

# 3. Instalar dependências e compilar
./mvnw clean install

# 4. Executar a aplicação
./mvnw spring-boot:run

# 5. (Opcional) abrir páginas de teste
# http://localhost:8080/firebase-test.html  -> login Firebase + chamada /api/auth/verify
# http://localhost:8080/gemini-tester.html -> upload currículo, jornada, chat
```
- Para usar outro `application.properties`, crie um perfil (`application-local.properties`) e rode com `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`.
- Certifique-se de que RabbitMQ esteja disponível ou adapte `spring.rabbitmq.*`.

## Execução de Testes
(- Testes automatizados atuais são básicos (`NextstepApplicationTests` apenas valida o contexto).
- Rodar: `./mvnw test`
- Há dependências de Testcontainers (SQL Server + RabbitMQ); habilite definindo `spring.testcontainers.enabled=true` caso queira usar os ambientes dockerizados para testes de integração.)

## Ambientes e Deploy
(Fazer devops primeiro)

## Estrutura de Pastas
```
nextstep/
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/softcode/nextstep
│   │   │   ├── api/{controller,dto}
│   │   │   ├── config/
│   │   │   ├── domain/
│   │   │   ├── exception/
│   │   │   ├── messaging/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/ (inclui subpacote `ai`)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── messages*.properties
│   │       └── static/{firebase-test.html, gemini-tester.html, signup.html}
│   └── test/ (Smoke tests e configs para Testcontainers)
└── mvnw / mvnw.cmd / .mvn
```
- `static/`: formulários rápidos para testar login Firebase, upload de currículo e chat Gemini.

## Boas Práticas & Padrões Utilizados
- Separação clara de camadas (Controller → Service → Repository) com DTOs imutáveis (`record`).
- Validação declarativa (`jakarta.validation`) em requests.
- Manipulação uniforme de erros via `GlobalExceptionHandler` + `ErrorResponse`.
- Segurança centralizada em filtro customizado (`FirebaseAuthenticationFilter`) + contexto de usuário.
- Integrações externas encapsuladas (`GeminiService`, `ResumeTextExtractor`, `NotificationProducer`).
- Uso de cache local (Caffeine) e internacionalização (`messages.properties`) para mensagens de erro.
- Eventos assíncronos publicados via RabbitMQ para desacoplar notificações do fluxo síncrono.

# API NextStep - Especificação Backend (Java + Spring Boot)

## 📋 Visão Geral
Sistema de requalificação profissional com IA que analisa currículos e gera trilhas personalizadas de aprendizado.

**Stack:**
- Backend: Java + Spring Boot
- Banco de Dados: Oracle SQL (gerenciado pelo Spring JPA)
- Autenticação: Firebase Authentication
- IA: Google Gemini API

---

## 🔥 FIREBASE AUTHENTICATION - Como Funciona

### Responsabilidades do Firebase:
- ✅ Registro de usuários (email/senha)
- ✅ Login/Logout
- ✅ Reset de senha (email automático)
- ✅ Verificação de email
- ✅ Login social (Google, Facebook, etc)
- ✅ Geração de tokens JWT

### Responsabilidades do Backend Java:
- ✅ **Extrair dados** do token JWT (uid, email)
- ✅ Armazenar e gerenciar TODOS os dados do app no Oracle
- ✅ Lógica de negócio (jornadas, análises, chat)
- ✅ Integração com Gemini API

### ⚠️ Nota de Implementação - Segurança

**Para facilitar avaliação com múltiplos projetos Firebase:**

- ✅ Backend **extrai dados do token JWT** sem validação Firebase Admin SDK
- ✅ Permite que avaliadores usem **qualquer projeto Firebase** configurado
- ✅ Adequado para **ambiente acadêmico/demonstração**
- ⚠️ Em **produção real**, recomenda-se usar Firebase Admin SDK para validação completa

**Fluxo simplificado:**
```
1. Frontend autentica com Firebase (qualquer projeto)
2. Obtém token JWT válido
3. Envia token para backend
4. Backend decodifica JWT e extrai uid + email
5. Busca/cria usuário no Oracle usando uid
```

---

## 🔐 FLUXO DE AUTENTICAÇÃO COMPLETO

### 1. Frontend (React) faz login no Firebase
```javascript
// Usuario digita email e senha
const userCredential = await signInWithEmailAndPassword(auth, email, password);

// Firebase retorna:
{
  uid: "abc123xyz",           // ← UID único do Firebase
  email: "teste@email.com",
  emailVerified: true
}

// Frontend pega o TOKEN JWT do Firebase
const token = await userCredential.user.getIdToken();
// token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 2. Frontend envia token para o Backend Java
```javascript
fetch('http://localhost:8080/api/auth/verify', {
  headers: {
    'Authorization': 'Bearer ' + token  // ← Token do Firebase
  }
})
```

### 3. Backend Java extrai dados e busca/cria usuário
```
1. Interceptor pega o token do header Authorization
2. Decodifica JWT manualmente (sem validação Admin SDK)
3. Extrai firebaseUid e email do payload do token
4. Busca usuário no Oracle: SELECT * FROM users WHERE firebase_uid = ?
5. Se não existir, cria novo: INSERT INTO users (firebase_uid, email)
6. Retorna dados do Oracle para o frontend
```

**Exemplo de extração do token:**
```java
// TokenService.java
public UserData extractUserFromToken(String token) {
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getDecoder().decode(parts[1]));
    
    JSONObject json = new JSONObject(payload);
    String uid = json.getString("user_id");  // Firebase UID
    String email = json.getString("email");
    
    return new UserData(uid, email);
}
```

### 4. Backend retorna dados do Oracle
```json
{
  "userId": "uuid-456",
  "firebaseUid": "abc123xyz",  // ← Chave que conecta Firebase e Oracle
  "name": "Cauã Santos",
  "email": "teste@email.com",
  "currentJob": "Desenvolvedor Frontend",
  "hasActiveJourney": true
}
```

---

## 🔑 IMPORTANTE: Firebase UID como Chave

O **Firebase UID** é a ponte entre autenticação e dados:

```
Firebase Authentication          Backend Oracle
      ↓                               ↓
  uid: "abc123xyz"    ←→    firebase_uid: "abc123xyz"
                                      ↓
                        [Busca todos os dados do usuário]
```

**Na tabela USERS do Oracle:**
```sql
id                  | firebase_uid | email            | name          | current_job
--------------------|--------------|------------------|---------------|--------------
uuid-456            | abc123xyz    | teste@email.com  | Cauã Santos   | Dev Frontend
```

---

## 🛠️ SETUP NECESSÁRIO NO BACKEND

### 1. Adicionar Dependências (pom.xml)
```xml
<!-- Para decodificar JWT -->
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20231013</version>
</dependency>

<!-- Spring Boot Starter Web (já deve ter) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 2. Criar Serviço de Extração de Token
```java
@Service
public class TokenService {
    
    public UserData extractUserFromToken(String token) {
        try {
            // Decodifica JWT sem validar assinatura
            String[] parts = token.split("\\.");
            String payload = new String(
                Base64.getDecoder().decode(parts[1])
            );
            
            JSONObject json = new JSONObject(payload);
            String uid = json.getString("user_id");
            String email = json.getString("email");
            
            return new UserData(uid, email);
            
        } catch (Exception e) {
            throw new UnauthorizedException("Token inválido");
        }
    }
}
```

### 3. Criar Filtro de Autenticação
- Interceptar TODAS as requisições (exceto `/api/auth/verify`)
- Extrair token do header `Authorization: Bearer {token}`
- Decodificar JWT e extrair `firebaseUid` e `email`
- Salvar no contexto da requisição (@RequestAttribute)
- **NÃO valida com Firebase Admin SDK** (aceita qualquer Firebase)

### 4. Entidades JPA
- Criar entidade `User` com campo `firebaseUid` (unique)
- Spring JPA cria as tabelas automaticamente no Oracle
- Relacionamentos: User → Journey → JourneyStep

---

## 🔐 1. AUTENTICAÇÃO (Firebase)

### POST `/api/auth/verify`
**Descrição:** Valida token do Firebase e retorna/cria usuário no Oracle

**Headers:**
```
Authorization: Bearer {firebase-jwt-token}
```

**Request Body:** (vazio)

**Fluxo Interno (Backend):**
1. Extrai token do header `Authorization`
2. Decodifica JWT (Base64 decode do payload)
3. Extrai `firebaseUid` e `email` do JSON
4. Busca usuário no Oracle: `SELECT * FROM users WHERE firebase_uid = ?`
5. Se não existir: `INSERT INTO users (firebase_uid, email) VALUES (?, ?)`
6. Retorna dados do Oracle

**Código de exemplo:**
```java
@PostMapping("/api/auth/verify")
public ResponseEntity<?> verify(
    @RequestHeader("Authorization") String auth
) {
    String token = auth.replace("Bearer ", "");
    UserData data = tokenService.extractUserFromToken(token);
    
    // Busca ou cria usuário
    User user = userRepository
        .findByFirebaseUid(data.getUid())
        .orElseGet(() -> {
            User newUser = new User();
            newUser.setFirebaseUid(data.getUid());
            newUser.setEmail(data.getEmail());
            return userRepository.save(newUser);
        });
    
    return ResponseEntity.ok(user);
}
```

**Response (200 OK):**
```json
{
  "userId": "uuid-123",
  "firebaseUid": "abc123xyz",
  "email": "caua@example.com",
  "name": null,
  "currentJob": null,
  "hasActiveJourney": false,
  "createdAt": "2025-11-12T10:30:00Z"
}
```

**Observações:**
- `name` e `currentJob` serão `null` no primeiro acesso
- Frontend deve detectar isso e redirecionar para completar perfil
- O **registro/login/reset de senha** acontecem no Firebase (frontend)

---

### POST `/api/auth/complete-profile`
**Descrição:** Completar perfil após primeiro login (nome, profissão)

**Headers:**
```
Authorization: Bearer {firebase-jwt-token}
```

**Request Body:**
```json
{
  "name": "Cauã Santos",
  "currentJob": "Desenvolvedor Frontend"
}
```

**Fluxo Interno:**
1. Valida token
2. Extrai `firebaseUid`
3. Executa: `UPDATE users SET name = ?, current_job = ? WHERE firebase_uid = ?`

**Response (200 OK):**
```json
{
  "userId": "uuid-123",
  "name": "Cauã Santos",
  "currentJob": "Desenvolvedor Frontend",
  "updatedAt": "2025-11-12T10:35:00Z"
}
```

---

## 📄 2. ANÁLISE DE CURRÍCULO

### POST `/api/resume/upload`
**Descrição:** Upload e análise de currículo com Gemini AI

**Headers:**
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request Body (FormData):**
```
file: curriculo.pdf (arquivo)
```

**Response (200 OK):**
```json
{
  "analysisId": "uuid-456",
  "summary": {
    "currentSkills": [
      {"name": "JavaScript", "level": "Avançado"},
      {"name": "React", "level": "Intermediário"},
      {"name": "Node.js", "level": "Básico"},
      {"name": "UX Design", "level": "Intermediário"}
    ],
    "experienceLevel": "Pleno",
    "currentJob": "Desenvolvedor Frontend",
    "yearsOfExperience": 3,
    "gaps": [
      "TypeScript avançado",
      "Arquitetura de Software",
      "Testes automatizados"
    ],
    "suggestedCareers": [
      {
        "title": "Full Stack Developer",
        "match": "92%",
        "reason": "Suas habilidades em JS e React são forte base. Falta backend."
      },
      {
        "title": "Product Designer",
        "match": "85%",
        "reason": "Conhecimento em UX Design + experiência em frontend."
      },
      {
        "title": "Frontend Architect",
        "match": "78%",
        "reason": "Experiência sólida, mas falta arquitetura e patterns."
      }
    ]
  },
  "analyzedAt": "2025-11-12T10:35:00Z"
}
```

---

### GET `/api/resume/analysis/{userId}`
**Descrição:** Buscar última análise de currículo

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "analysisId": "uuid-456",
  "summary": { /* mesmo objeto do upload */ },
  "analyzedAt": "2025-11-12T10:35:00Z"
}
```

---

## 🎯 3. JORNADAS

### POST `/api/journeys/generate`
**Descrição:** Gerar nova jornada personalizada com Gemini AI

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "desiredJob": "Full Stack Developer",
  "currentSkills": ["JavaScript", "React", "Node.js"],
  "gaps": ["TypeScript", "Databases", "Testing"]
}
```

**Response (201 Created):**
```json
{
  "journeyId": "uuid-789",
  "desiredJob": "Full Stack Developer",
  "totalSteps": 4,
  "estimatedTime": "18 semanas",
  "overallProgress": 0,
  "status": "active",
  "steps": [
    {
      "stepId": "step-1",
      "order": 1,
      "title": "Fundamentos de TypeScript",
      "objective": "Dominar tipagem estática e features avançadas do TS",
      "resources": "TypeScript Handbook, Udemy - Understanding TypeScript",
      "estimatedTime": "4 semanas",
      "progress": 0,
      "status": "pending"
    },
    {
      "stepId": "step-2",
      "order": 2,
      "title": "Banco de Dados SQL/NoSQL",
      "objective": "Aprender PostgreSQL e MongoDB",
      "resources": "FreeCodeCamp SQL, MongoDB University",
      "estimatedTime": "5 semanas",
      "progress": 0,
      "status": "pending"
    },
    {
      "stepId": "step-3",
      "order": 3,
      "title": "APIs REST e GraphQL",
      "objective": "Criar APIs robustas e escaláveis",
      "resources": "REST API Design, Apollo GraphQL Tutorial",
      "estimatedTime": "4 semanas",
      "progress": 0,
      "status": "pending"
    },
    {
      "stepId": "step-4",
      "order": 4,
      "title": "Testing e CI/CD",
      "objective": "Implementar testes automatizados e deploy contínuo",
      "resources": "Jest Documentation, GitHub Actions Tutorial",
      "estimatedTime": "5 semanas",
      "progress": 0,
      "status": "pending"
    }
  ],
  "insights": [
    {
      "type": "skill",
      "icon": "lightbulb",
      "text": "Nova habilidade em alta: Edge Computing"
    },
    {
      "type": "trend",
      "icon": "trending-up",
      "text": "Tendência crescente: JAMStack Architecture"
    },
    {
      "type": "certification",
      "icon": "award",
      "text": "Certificação recomendada: AWS Certified Developer"
    }
  ],
  "createdAt": "2025-11-12T10:40:00Z"
}
```

---

### GET `/api/journeys/active`
**Descrição:** Buscar jornada ativa do usuário

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "journeyId": "uuid-789",
  "desiredJob": "Full Stack Developer",
  "totalSteps": 4,
  "completedSteps": 1,
  "estimatedTime": "18 semanas",
  "overallProgress": 35,
  "status": "active",
  "nextStep": {
    "stepId": "step-2",
    "title": "Banco de Dados SQL/NoSQL",
    "progress": 60
  },
  "steps": [ /* array completo de steps */ ],
  "insights": [ /* array de insights */ ],
  "createdAt": "2025-11-12T10:40:00Z",
  "updatedAt": "2025-11-13T15:20:00Z"
}
```

---

### PATCH `/api/journeys/steps/{stepId}/progress`
**Descrição:** Atualizar progresso de uma etapa

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "progress": 75
}
```

**Response (200 OK):**
```json
{
  "stepId": "step-2",
  "title": "Banco de Dados SQL/NoSQL",
  "progress": 75,
  "status": "in-progress",
  "updatedAt": "2025-11-13T15:25:00Z"
}
```

---

### GET `/api/journeys/history`
**Descrição:** Histórico de jornadas concluídas

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "journeys": [
    {
      "journeyId": "uuid-111",
      "desiredJob": "Product Designer",
      "completedAt": "2025-10-15T10:00:00Z",
      "overallProgress": 100,
      "totalSteps": 5
    },
    {
      "journeyId": "uuid-222",
      "desiredJob": "Frontend Developer",
      "completedAt": "2025-08-20T12:30:00Z",
      "overallProgress": 100,
      "totalSteps": 4
    }
  ]
}
```

---

## 🏠 4. HOME / DASHBOARD

### GET `/api/dashboard`
**Descrição:** Dados para a tela Home

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "user": {
    "name": "Cauã Santos",
    "currentJob": "Desenvolvedor Frontend",
    "desiredJob": "Full Stack Developer"
  },
  "nextStep": {
    "title": "Banco de Dados SQL/NoSQL",
    "objective": "Aprender PostgreSQL e MongoDB",
    "progress": 60
  },
  "skills": [
    {"name": "JavaScript", "progress": 85},
    {"name": "React", "progress": 78},
    {"name": "UX Design", "progress": 65},
    {"name": "TypeScript", "progress": 45}
  ],
  "trends": [
    {"title": "IA Generativa", "icon": "bot"},
    {"title": "Web3", "icon": "zap"},
    {"title": "DevOps", "icon": "settings"},
    {"title": "Blockchain", "icon": "blocks"},
    {"title": "Mobile First", "icon": "smartphone"},
    {"title": "Cloud Native", "icon": "cloud"}
  ],
  "suggestedPaths": [
    {"title": "Full Stack Developer", "match": "92%"},
    {"title": "Product Designer", "match": "85%"},
    {"title": "Frontend Architect", "match": "78%"}
  ]
}
```

---

## 💬 5. CHAT (Mentor AI)

### POST `/api/chat/send`
**Descrição:** Enviar mensagem para o Mentor AI

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "message": "Como melhorar em TypeScript?",
  "conversationId": "conv-123"
}
```

**Response (200 OK):**
```json
{
  "messageId": "msg-456",
  "conversationId": "conv-123",
  "role": "ai",
  "message": "Ótima pergunta! Para melhorar em TypeScript, recomendo:\n\n1. **Pratique tipagem avançada**: Explore Generics, Utility Types e Type Guards\n2. **Leia o handbook oficial**: https://www.typescriptlang.org/docs/\n3. **Faça projetos reais**: Converta seus projetos JS para TS\n\nQuer que eu sugira alguns exercícios práticos?",
  "timestamp": "2025-11-12T11:00:00Z"
}
```

---

### GET `/api/chat/history?conversationId={id}&limit=50`
**Descrição:** Histórico de mensagens

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "conversationId": "conv-123",
  "messages": [
    {
      "messageId": "msg-1",
      "role": "ai",
      "message": "Olá! Sou o Mentor AI...",
      "timestamp": "2025-11-12T10:50:00Z"
    },
    {
      "messageId": "msg-2",
      "role": "user",
      "message": "Quais habilidades devo desenvolver?",
      "timestamp": "2025-11-12T10:51:00Z"
    },
    {
      "messageId": "msg-3",
      "role": "ai",
      "message": "Com base no seu perfil...",
      "timestamp": "2025-11-12T10:51:30Z"
    }
  ]
}
```

---

## 👤 6. PERFIL

### GET `/api/profile`
**Descrição:** Dados do perfil do usuário

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "userId": "uuid-123",
  "name": "Cauã Santos",
  "email": "caua@example.com",
  "currentJob": "Desenvolvedor Frontend",
  "profilePicture": null,
  "createdAt": "2025-10-01T08:00:00Z",
  "stats": {
    "totalJourneys": 3,
    "completedJourneys": 2,
    "totalSkills": 12,
    "averageProgress": 68
  }
}
```

---

### PUT `/api/profile`
**Descrição:** Atualizar perfil

**Headers:**
```
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "name": "Cauã Silva Santos",
  "currentJob": "Full Stack Developer",
  "email": "caua.novo@example.com"
}
```

**Response (200 OK):**
```json
{
  "userId": "uuid-123",
  "name": "Cauã Silva Santos",
  "email": "caua.novo@example.com",
  "currentJob": "Full Stack Developer",
  "updatedAt": "2025-11-12T11:30:00Z"
}
```

---

### DELETE `/api/profile`
**Descrição:** Excluir conta permanentemente

**Headers:**
```
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "message": "Conta excluída com sucesso",
  "success": true
}
```

---

## 🔍 7. PROFISSÕES (Para Nova Jornada)

### GET `/api/professions/suggested`
**Descrição:** Profissões sugeridas baseadas no perfil

**Headers:**
```
Authorization: Bearer {token}
```

**Query Params:**
```
?search=designer (opcional)
```

**Response (200 OK):**
```json
{
  "professions": [
    {
      "id": "prof-1",
      "title": "Product Designer",
      "category": "Design",
      "match": "92%",
      "description": "Profissional que une UX/UI com visão de produto"
    },
    {
      "id": "prof-2",
      "title": "Full Stack Developer",
      "category": "Tecnologia",
      "match": "88%",
      "description": "Desenvolvedor completo, frontend + backend"
    },
    {
      "id": "prof-3",
      "title": "UX Researcher",
      "category": "Design",
      "match": "85%",
      "description": "Especialista em pesquisa com usuários"
    },
    {
      "id": "prof-4",
      "title": "Data Scientist",
      "category": "Dados",
      "match": "78%",
      "description": "Analista de dados com foco em ML/IA"
    }
  ]
}
```

---

## 🚨 8. TRATAMENTO DE ERROS

Todos os endpoints devem retornar erros padronizados:

### 400 - Bad Request
```json
{
  "error": "BAD_REQUEST",
  "message": "Dados inválidos no corpo da requisição",
  "details": {
    "email": "Email inválido",
    "password": "Senha deve ter no mínimo 6 caracteres"
  }
}
```

### 401 - Unauthorized
```json
{
  "error": "UNAUTHORIZED",
  "message": "Token inválido ou expirado"
}
```

### 404 - Not Found
```json
{
  "error": "NOT_FOUND",
  "message": "Recurso não encontrado"
}
```

### 500 - Internal Server Error
```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Erro interno do servidor",
  "requestId": "req-uuid-789"
}
```

---

## 🔧 OBSERVAÇÕES TÉCNICAS

### Autenticação
- **Firebase Authentication** gerencia senhas, reset, verificação de email
- Frontend usa Firebase SDK para login/registro
- Backend **extrai dados do token JWT** sem validação Admin SDK
- Tokens são decodificados automaticamente em cada request via Filtro
- Header obrigatório: `Authorization: Bearer {firebase-jwt-token}`
- `firebaseUid` é a chave que conecta Firebase Auth ↔ Oracle
- **Aceita qualquer projeto Firebase** configurado no frontend

### Como Backend Identifica o Usuário
1. Frontend envia token JWT do Firebase no header `Authorization`
2. Filtro do Spring intercepta e decodifica token (Base64)
3. Extrai `firebaseUid` e `email` do payload JSON
4. Busca/cria usuário no Oracle: `userRepository.findByFirebaseUid(uid)`
5. Todas as operações usam esse usuário autenticado
6. **Funciona com qualquer Firebase** (não precisa configurar Admin SDK)

### Vantagens desta Abordagem
- ✅ **Zero configuração** de Firebase no backend
- ✅ Avaliadores podem usar **qualquer projeto Firebase**
- ✅ Backend **independente** de credenciais específicas
- ✅ Simples de deployar (Azure, Railway, Heroku)
- ✅ Dados salvos corretamente no Oracle por usuário

### Rate Limiting
- 100 requisições/min por usuário
- 15 requisições/min para Gemini (tier gratuito)

### Gemini API
- **Model**: `gemini-1.5-flash` (gratuito, rápido)
- **Temperature**: 0.7 (criativo mas consistente)
- **Response Format**: JSON mode quando possível
- **Fallback**: Cache de respostas comuns

### Banco de Dados
- **Oracle SQL** gerenciado pelo Spring JPA
- Spring cria as tabelas automaticamente (DDL auto)
- Não precisa escrever SQL manualmente
- Apenas criar entidades JPA e repositories

### Arquivos
- Upload máximo: **5MB**
- Formatos aceitos: **PDF, DOC, DOCX**
- Storage: **AWS S3** ou **Google Cloud Storage**

---

## 📦 ENTIDADES PRINCIPAIS

```
User
├── id (UUID)
├── firebaseUid (String, unique) ← UID do Firebase
├── name
├── email (unique)
├── currentJob
├── createdAt
└── updatedAt

ResumeAnalysis
├── id (UUID)
├── userId (FK)
├── skillsJson (JSON)
├── gapsJson (JSON)
├── suggestedCareersJson (JSON)
└── analyzedAt

Journey
├── id (UUID)
├── userId (FK)
├── desiredJob
├── status (active/completed/archived)
├── overallProgress (0-100)
├── createdAt
└── updatedAt

JourneyStep
├── id (UUID)
├── journeyId (FK)
├── order
├── title
├── objective
├── resources
├── estimatedTime
├── progress (0-100)
└── status (pending/in-progress/completed)

ChatMessage
├── id (UUID)
├── userId (FK)
├── conversationId
├── role (user/ai)
├── message
└── timestamp
```

---

## 🎯 PRIORIDADE DE IMPLEMENTAÇÃO

### FASE 1 (MVP)
1. ✅ Auth (register, login)
2. ✅ Upload + análise de currículo (Gemini)
3. ✅ Gerar jornada (Gemini)
4. ✅ Dashboard básico

### FASE 2
5. ✅ Chat com Mentor AI
6. ✅ Atualizar progresso de steps
7. ✅ Editar perfil

### FASE 3
8. ✅ Histórico de jornadas
9. ✅ Insights personalizados
10. ✅ Novas profissões sugeridas

---

**Qualquer dúvida, me chama! 🚀**

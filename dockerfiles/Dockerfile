# Dockerfile multi-stage para construir e executar a aplicação Java (Java 17)
# Fase de build: usa Maven 3.9.9 com Eclipse Temurin 17 para compilar o JAR
# Fase de run: usa Eclipse Temurin 17 JRE para executar o JAR final

# ----------------------
# Etapa 1: Build (Maven)
# ----------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build

# Define diretório de trabalho dentro da imagem de build
WORKDIR /app

# Copia arquivos de definição do projeto primeiro para aproveitar cache de dependências
COPY pom.xml ./

# Copia o código-fonte da aplicação
COPY src ./src

# Compila o projeto gerando o JAR, pulando os testes
RUN mvn -DskipTests clean package

# ----------------------
# Etapa 2: Runtime (JRE)
# ----------------------
FROM eclipse-temurin:17-jre AS run

# Define diretório de trabalho da aplicação
WORKDIR /opt/app

# Copia o JAR construído da etapa de build
# Requisito: copiar o *-SNAPSHOT.jar para /opt/app/app.jar
COPY --from=build /app/target/*-SNAPSHOT.jar /opt/app/app.jar

# Expõe a porta padrão da aplicação
EXPOSE 8080

# Define o comando de entrada para executar o JAR
ENTRYPOINT ["java", "-jar", "app.jar"]

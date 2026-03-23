ARG BUILDER_IMAGE=maven:3.9.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre

FROM ${BUILDER_IMAGE} AS builder
WORKDIR /app

# 1. 先复制 pom.xml 并下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B -DskipTests || mvn dependency:resolve -B

# 2. 再复制源代码并编译
COPY src ./src
RUN mvn -B -DskipTests package

FROM ${RUNTIME_IMAGE}
WORKDIR /app
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

# 多阶段构建：Maven 打包 -> JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY exam-common ./exam-common
COPY exam-model ./exam-model
COPY exam-mapper ./exam-mapper
COPY exam-service ./exam-service
COPY exam-admin ./exam-admin
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/exam-admin/target/exam-server.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

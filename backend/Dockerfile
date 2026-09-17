FROM docker.m.daocloud.io/library/eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY src ./src

# Prefer Aliyun Maven mirror for reliable builds in CN networks
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'EOF'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

RUN mvn -q -DskipTests package

FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

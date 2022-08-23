FROM maven:3-jdk-11-slim AS build

WORKDIR /pwd

COPY ./pom.xml /pwd/

RUN mvn package -Dspring-boot.repackage.skip=true

COPY ./src /pwd/src
RUN mvn package -DskipTests=true

FROM openjdk:11-jre-slim

COPY --from=build /pwd/target/*.jar /usr/src/app/main.jar

WORKDIR /usr/src/app
CMD java $JAVA_OPTS -jar main.jar

FROM azul/zulu-openjdk-alpine:18-latest as builder
RUN apk update
RUN apk add wget zip
RUN wget https://services.gradle.org/distributions/gradle-7.5.1-bin.zip
RUN mkdir /opt/gradle
RUN unzip -d /opt/gradle gradle-7.5.1-bin.zip
ENV PATH=$PATH:/opt/gradle/gradle-7.5.1/bin
WORKDIR /app
COPY src src
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
RUN gradle build

FROM azul/zulu-openjdk-alpine:18-latest
WORKDIR /app
COPY resources resources
RUN mkdir database
COPY --from=builder /app/build/libs/notifier.jar .
ENTRYPOINT ["java", "-jar","notifier.jar"]
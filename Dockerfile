FROM bellsoft/liberica-openjdk-alpine:18 as builder
RUN apk update
RUN apk add wget zip
RUN wget https://services.gradle.org/distributions/gradle-7.5.1-bin.zip
RUN mkdir /opt/gradle
RUN unzip -d /opt/gradle gradle-7.5.1-bin.zip
ENV PATH=$PATH:/opt/gradle/gradle-7.5.1/bin
WORKDIR /app
COPY src src
COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
RUN gradle build

FROM bellsoft/liberica-openjre-alpine:18
WORKDIR /app
RUN mkdir database
COPY resources resources
COPY --from=builder /app/build/libs/notifier.jar .
ENTRYPOINT ["java", "-jar", "notifier.jar"]
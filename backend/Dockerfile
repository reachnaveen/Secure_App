# Use a build argument to specify the OpenTelemetry agent version
ARG OTEL_AGENT_VERSION=1.32.0
# Download the OpenTelemetry agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

FROM eclipse-temurin:17-jre-focal
VOLUME /tmp
# Copy the OpenTelemetry agent from the builder stage
COPY --from=0 /opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

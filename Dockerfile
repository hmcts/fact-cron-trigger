 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.1
FROM hmctsprod.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/fact-cron-trigger.jar /opt/app/

USER 65532:65532

EXPOSE 8050
CMD [ "fact-cron-trigger.jar" ]

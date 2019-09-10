FROM openjdk:8
WORKDIR /sangria
COPY target/scala-2.12/tdr-api.jar /sangria
CMD java -jar /sangria/tdr-api.jar
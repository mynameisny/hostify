FROM harbor.ctl.cloud.cnpc/library/java-21:20250331

RUN mkdir -p /app/

COPY build/libs/*.jar /app/

WORKDIR /app/

CMD java -jar hostify-0.0.1-SNAPSHOT.jar

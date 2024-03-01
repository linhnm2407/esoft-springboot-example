FROM openjdk:18
ADD target/esoft-*.jar esoft.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","esoft.jar"]
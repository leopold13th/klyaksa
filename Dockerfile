FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/klyaksa.jar /klyaksa/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/klyaksa/app.jar"]

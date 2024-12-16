FROM pr2:latest

WORKDIR /opt/nondeterminism

COPY . .

CMD ["./gradlew"]
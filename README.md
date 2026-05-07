# Hospital Management — Development quickstart

This repo is a Spring Boot app (Java 17, Maven). By default `application.properties` points to a local MySQL instance. For local development you can either run a MySQL container or run with the `test` profile (H2).

## Prerequisites
- Java 17 (Temurin/OpenJDK)
- Maven
- Docker (optional, recommended to run MySQL easily)

## Java 17 — set for the session and persist
Temporarily for current shell (macOS):

```bash
export JAVA_HOME="$($(/usr/libexec/java_home -v 17))"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

To persist, add the two export lines to `~/.zshrc`.

## Option A — Run MySQL locally using Docker (recommended)
Start MySQL (the project includes `docker-compose.yml`):

```bash
# from repository root
docker compose up -d
# wait for the container to become healthy (healthcheck)
docker compose ps
```

Verify DB exists:

```bash
# run a command inside the container to show databases
docker exec -it hospital-mysql mysql -uroot -pRiriri1231 -e "SHOW DATABASES;"
```

Then run the app (ensure JAVA_HOME points to Java 17):

```bash
mvn spring-boot:run
```

or explicitly with profile `default` (application.properties uses MySQL):

```bash
mvn -Dspring-boot.run.profiles=default spring-boot:run
```

If you prefer to use a non-root user, update `application.properties` with that username/password or change `docker/mysql-init/init.sql` to create a user and privileges.

## Option B — Run the app with the `test` profile (H2 in-memory)
Quick start without MySQL:

```bash
mvn -Dspring-boot.run.profiles=test spring-boot:run
```

This uses `src/main/resources/application-test.properties` and the bundled H2 driver (declared runtime scope in `pom.xml`).

## Troubleshooting
- `Access denied for user 'root'@'localhost'` — verify the password in `application.properties` matches the DB. Use the `docker exec` command above to confirm.
- If you changed the DB or credentials, restart the app after updating `application.properties`.
- Ensure Maven and the running `java` are using Java 17.

## Next steps I can do for you
- Create a dedicated non-root DB user and update `application.properties` accordingly.
- Add a small SQL migration (Flyway/liquibase) to bootstrap schema for development.
- Add a `Makefile` or scripts to simplify `docker compose up` + `mvn spring-boot:run`.


FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6

WORKDIR /app
COPY . /app

# Construir el proyecto
RUN sbt clean compile stage

# Exponer el puerto que usa tu aplicación
EXPOSE 8080

# Comando para iniciar la aplicación
CMD ["./target/universal/stage/bin/recipe-api"]
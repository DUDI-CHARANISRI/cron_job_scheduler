Receiver microservice
=====================

This is a tiny Spring Boot receiver service that accepts POSTs at `/hooks/process` and
stores them in memory for demo purposes. It exposes `/hooks/received` to list received payloads.

Run locally:

```bash
cd receiver
./mvnw spring-boot:run
```

Create a job in the main scheduler pointing to `http://localhost:8080/hooks/process` (if you run both services locally make sure ports don't conflict) or update the receiver port with `--server.port=8081`.

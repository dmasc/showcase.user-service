# Showcase user-service
This project provides a simple user service exposing REST endpoints for user management.
While the business logic is intentionally minimal and not the main focus, the project demonstrates 
how the following technologies can be used in application development:

- Flyway for database schema management and initialization
- JSON Web Tokens (JWT) for authorization
- A Dockerfile for building a containerized application image
- A GitHub Actions pipeline for CI/CD purposes

For simplicity:

- Entities are used directly instead of introducing DTOs
- The GitHub pipeline does not deploy the application automatically, as deployment is intended to be 
performed manually, primarily in local environments
 
## How to build and run the application

### Build and run the Docker container locally
In order to build and run a Docker image, a Docker distribution needs to be installed on your system.

Use the following command in the root directory of the project to build a Docker image for this application:
```
docker build -t showcase.user-service .
```

Start the Docker container with the following command:
```
docker run -p 8080:8080 showcase.user-service
```

### Pull and run the Docker image from the GitHub Container Registry (GHCR)
A Docker image of the application is available in the GHCR and can be pulled with the following command:
```
docker pull ghcr.io/dmasc/showcase.user-service:latest
```

Start the Docker container with the following command:
```
docker run -p 8080:8080 ghcr.io/dmasc/showcase.user-service
```


## How to use the application
Currently, the application only provides two REST endpoints:

### Login endpoint (/login)

This endpoint returns a JSON Web Token when valid credentials are provided.
Valid credentials can be found in `data.sql` in the `resources` folder.

To retrieve an authorization token, send a POST request with a JSON body containing a valid username and password.

Example request:
```
POST http://localhost:8080/login
Content-Type: application/json

{
  "username": "Dennis",
  "password": "dennis-pw"
}
```

### User endpoint (/user)
Use this endpoint to retrieve user data by user ID.

Include the JSON Web Token retrieved by the `login` request in the `Authorization` header, prefixed with `Bearer `.

Refer to the `data.sql` file in the `resources` folder to see which users are available.

Example request:
```
GET http://localhost:8080/user?id=2
Authorization: Bearer <YOUR TOKEN>
```

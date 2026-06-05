# Showcase user-service
This project contains a simple user service that provides REST interfaces to manage users. 
While the simple business logic of the service is not the main focus of this project, it is 
intended to show how the following technologies could be used in the development of applications:

- Flyway to set up the database.
- JSON web tokens (JWT) for authorization.
- Dockerfile to create a Docker application image.

For simplicity reasons:

- Entities are used directly instead of DTOs.

 
## Build and run the Docker container
To build a Docker image containing the application, use the following command in the root directory 
of the project:
```
docker build -t showcase.user-service .
```

Start the Docker image with the following command:
```
docker run -p 8080:8080 showcase.user-service
```

## How to use the application
Currently, the application only provides two REST endpoints:

### Login endpoint (/login)

This endpoint returns a JSON Web Token when valid credentials are provided.
Valid credentials can be found in `data.sql` in the `resources` folder.

To retrieve an authorization token, send a POST request with a JSON body containing the username and password.

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

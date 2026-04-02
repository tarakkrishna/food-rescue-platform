# Food Rescue Backend

A Spring Boot application for managing food rescue operations between restaurants and NGOs.

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+

## Database Setup

1. Create MySQL database (or let Hibernate auto-create):
```sql
CREATE DATABASE IF NOT EXISTS food_rescue;
```

2. Update database credentials in `application.properties` if needed:
```properties
spring.datasource.username=root
spring.datasource.password=root
```

## Running the Application

### Option 1: Using Maven
```bash
mvn clean install
mvn spring-boot:run
```

### Option 2: Run JAR
```bash
mvn clean package
java -jar target/food-rescue-backend-1.0.0.jar
```

The server starts on `http://localhost:8080`

## API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/auth/register` | Register new user | `{"name": "", "email": "", "password": "", "role": "RESTAURANT/NGO"}` |
| POST | `/auth/login` | Login and get JWT | `{"email": "", "password": ""}` |
| OPTIONS | `/**` | CORS preflight | - |

### Restaurant Endpoints (Requires ROLE_RESTAURANT)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| POST | `/restaurant/add-food` | Add new food item | `{"name": "", "quantity": "", "expiryDate": "YYYY-MM-DD"}` |
| GET | `/restaurant/food-list` | List all food items by this restaurant | - |

### NGO Endpoints (Requires ROLE_NGO)

| Method | Endpoint | Description | Request Body |
|--------|----------|-------------|--------------|
| GET | `/food/available` | List all available food items | - |
| POST | `/food/claim/{id}` | Claim a food item by ID | - |
| GET | `/food/my-claims` | List claims made by this NGO | - |

### Shared Endpoints (NGO or Restaurant)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/food/available` | View available food (both roles) |

## Authentication

All protected endpoints require a Bearer token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

## Testing with cURL

### 1. Register a Restaurant
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Restaurant", "email": "restaurant@test.com", "password": "password123", "role": "RESTAURANT"}'
```

### 2. Register an NGO
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Test NGO", "email": "ngo@test.com", "password": "password123", "role": "NGO"}'
```

### 3. Login as Restaurant
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "restaurant@test.com", "password": "password123"}'
```
Response:
```json
{
  "token": "eyJhbG...",
  "type": "Bearer",
  "id": 1,
  "name": "Test Restaurant",
  "email": "restaurant@test.com",
  "role": "RESTAURANT"
}
```

### 4. Add Food (Restaurant)
```bash
curl -X POST http://localhost:8080/restaurant/add-food \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name": "Fresh Bread", "quantity": "10 loaves", "expiryDate": "2024-12-31"}'
```

### 5. List Restaurant's Food
```bash
curl -X GET http://localhost:8080/restaurant/food-list \
  -H "Authorization: Bearer <restaurant_token>"
```

### 6. View Available Food (NGO or Restaurant)
```bash
curl -X GET http://localhost:8080/food/available \
  -H "Authorization: Bearer <token>"
```

### 7. Claim Food (NGO)
```bash
curl -X POST http://localhost:8080/food/claim/1 \
  -H "Authorization: Bearer <ngo_token>"
```

### 8. View NGO's Claims
```bash
curl -X GET http://localhost:8080/food/my-claims \
  -H "Authorization: Bearer <ngo_token>"
```

## Response Formats

### Success Response (JWT)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "id": 1,
  "name": "Test Restaurant",
  "email": "restaurant@test.com",
  "role": "RESTAURANT"
}
```

### Food Item Response
```json
{
  "id": 1,
  "name": "Fresh Bread",
  "quantity": "10 loaves",
  "expiryDate": "2024-12-31",
  "status": "AVAILABLE",
  "restaurantId": 1,
  "restaurantName": "Test Restaurant"
}
```

### Claim Response
```json
{
  "id": 1,
  "foodItemId": 1,
  "foodItemName": "Fresh Bread",
  "ngoId": 2,
  "ngoName": "Test NGO",
  "claimDate": "2024-03-12T10:30:00",
  "status": "PENDING"
}
```

### Error Response (401 Unauthorized)
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/restaurant/food-list"
}
```

### Error Response (403 Forbidden)
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: Access is denied",
  "path": "/restaurant/add-food"
}
```

## CORS Configuration

CORS is configured to allow requests from `http://localhost:3000` (frontend). The following headers and methods are allowed:

**Origins:** `http://localhost:3000`
**Methods:** `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
**Headers:** `Authorization`, `Content-Type`, `Accept`
**Credentials:** Enabled

## Database Schema

### Users Table
- `id` (PK, BIGINT, AUTO_INCREMENT)
- `name` (VARCHAR)
- `email` (VARCHAR, UNIQUE)
- `password` (VARCHAR)
- `role` (ENUM: RESTAURANT, NGO)

### Food Items Table
- `id` (PK, BIGINT, AUTO_INCREMENT)
- `restaurant_id` (FK, BIGINT)
- `name` (VARCHAR)
- `quantity` (VARCHAR)
- `expiry_date` (DATE)
- `status` (ENUM: AVAILABLE, CLAIMED)

### Claims Table
- `id` (PK, BIGINT, AUTO_INCREMENT)
- `food_item_id` (FK, BIGINT)
- `ngo_id` (FK, BIGINT)
- `claim_date` (DATETIME)
- `status` (ENUM: PENDING, APPROVED)

## Security Features

- **JWT Authentication**: Stateless token-based authentication
- **BCrypt Password Encoding**: Secure password hashing
- **Role-based Access Control**: Different endpoints for RESTAURANT and NGO roles
- **CORS Protection**: Configured for specific frontend origin
- **JWT Filter**: Validates tokens on each request
- **Custom Entry Point**: Returns JSON error for unauthorized access
- **Access Denied Handler**: Returns JSON error for forbidden access

## Environment Variables (Optional)

You can override default configurations using environment variables:

```bash
export DB_URL=jdbc:mysql://localhost:3306/food_rescue
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export JWT_SECRET=yourSecretKeyMustBeLongEnoughForSecurity
export JWT_EXPIRATION=86400000
export SERVER_PORT=8080
```

## Project Structure

```
food-rescue-backend/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── foodrescue/
        │           ├── FoodRescueApplication.java
        │           ├── controller/
        │           │   ├── AuthController.java
        │           │   ├── FoodController.java
        │           │   └── RestaurantController.java
        │           ├── dto/
        │           │   ├── ClaimResponse.java
        │           │   ├── FoodItemRequest.java
        │           │   ├── FoodItemResponse.java
        │           │   ├── JwtResponse.java
        │           │   ├── LoginRequest.java
        │           │   ├── MessageResponse.java
        │           │   └── RegisterRequest.java
        │           ├── entity/
        │           │   ├── Claim.java
        │           │   ├── FoodItem.java
        │           │   └── User.java
        │           ├── repository/
        │           │   ├── ClaimRepository.java
        │           │   ├── FoodItemRepository.java
        │           │   └── UserRepository.java
        │           └── security/
        │               ├── config/
        │               │   └── SecurityConfig.java
        │               ├── handler/
        │               │   ├── AccessDeniedHandlerJwt.java
        │               │   └── AuthEntryPointJwt.java
        │               ├── jwt/
        │               │   ├── AuthTokenFilter.java
        │               │   └── JwtUtils.java
        │               └── service/
        │                   └── UserDetailsServiceImpl.java
        └── resources/
            └── application.properties
```

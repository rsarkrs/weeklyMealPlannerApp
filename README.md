# Weekly Meal Planner App

Backend service for generating budget-aware weekly meal plans using grocery pricing data (starting with Kroger API).

## Tech Stack

- Java 21
- Spring Boot 3
- Maven (with Maven Wrapper)

## Prerequisites

- Java 21 installed
- Git installed

## Getting Started

### 1) Clone the repo

```bash
git clone https://github.com/<your-username>/weeklyMealPlannerApp.git
cd weeklyMealPlannerApp
```
### 2) Set environment variables

Create a local `.env` file (do not commit), or set system environment variables.

Example `.env.example` contents:

```KROGER_CLIENT_ID=your_client_id_here
KROGER_CLIENT_SECRET=your_client_secret_here
KROGER_BASE_URL=https://api.kroger.com
```

PowerShell (current session only):

```$env:KROGER_CLIENT_ID="your_real_id"
$env:KROGER_CLIENT_SECRET="your_real_secret"
$env:KROGER_BASE_URL="https://api.kroger.com"
```


### 3) Build and test

#### Windows
.\mvnw.cmd clean test

#### macOS / Linux
./mvnw clean test

### 4) Run the app

#### Windows
.\mvnw.cmd spring-boot:run

#### macOS / Linux
./mvnw spring-boot:run

The app runs at:

- `http://localhost:8080`

Health endpoint:

- `http://localhost:8080/actuator/health`

## Testing

### Test matrix

- `KrogerControllerTest`: controller validation and HTTP status/body behavior
- `FindKrogerLocationsServiceTest`: mapping logic, hours formatting, null safety, pickup/delivery flags
- `KrogerAuthHttpClientTest`: token request contract, auth headers, caching, token error handling
- `KrogerLocationsHttpClientTest`: query params, bearer token header, response parsing, non-2xx handling
- `KrogerLocationsApiIntegrationTest`: end-to-end `/api/v1/kroger/locations` flow with mocked upstream token/locations APIs

### Run tests

Windows (all tests):

```powershell
.\mvnw.cmd test
```

Windows (integration test only):

```powershell
.\mvnw.cmd -Dtest=KrogerLocationsApiIntegrationTest test
```

Windows (Kroger unit tests only):

```powershell
.\mvnw.cmd -Dtest=KrogerControllerTest,FindKrogerLocationsServiceTest,KrogerAuthHttpClientTest,KrogerLocationsHttpClientTest test
```

macOS / Linux (all tests):

```bash
./mvnw test
```

## Planned Endpoints

- `GET /api/v1/kroger/locations`
- `GET /api/v1/kroger/products`
- `POST /api/v1/plans/generate` (future)

## Project Structure

src/
  main/
    java/com/rami/weeklymealplanner/
      common/
      config/
      kroger/
        api/
        application/
        domain/
        infrastructure/
  test/
    java/com/rami/weeklymealplanner/
      ...

## Roadmap

1. Kroger OAuth integration
2. Locations/products retrieval
3. Ingredient and unit normalization
4. Cheapest-basket builder
5. Weekly plan optimization

## Development Practices

- Use short-lived feature branches (e.g., `feat/kroger-auth`)
- Use conventional commit style:
  - `feat: ...`
  - `fix: ...`
  - `chore: ...`
  - `test: ...`
- Run tests before pushing

## Security

- Never commit real secrets
- Keep `.env` local only
- Commit `.env.example` with placeholder values only


## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

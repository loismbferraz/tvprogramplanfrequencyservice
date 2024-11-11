# Tv Program Plan Frequency Service Challenge

## Overview

After analyzing the problem, the following points were considered in the development of the application:

- **API with a single controller**: ShowsApi.
- **Decoupling the execution of provider requests** from the main application (non-blocking requests).
- **Non-blocking HTTP request library**: WebClient.
- **Queries**: Provide a set of queries to filter the results.


### Main Tools Used
- Spring Boot
- Project Reactor
- WebClient
- Docker
- Wiremock

##  Key Components

### 1. ShowsController (API Controller)
The `ShowsController` processes incoming HTTP requests and provides two main endpoints:
- `/api/shows/aggregatedbytvshow`: Retrieves TV shows and their airings aggregated by show for a specified date.
- `/api/shows/orderedbyoccurrences`: Returns TV show occurrences ordered by frequency within a specified date range, with options for ordering (`asc` or `desc`) and limiting the results.

Each endpoint performs **input validation** and uses the service layer to fetch data. Responses are converted to DTOs (Data Transfer Objects) to maintain a standardized format for clients.

### 2. EPGCacheService (Service Layer)
The `EPGCacheService` manages data retrieval and caching. Key responsibilities include:
- **getRawData**: Checks if data for a specific date is cached; if not, it fetches and caches the data from the provider.
- **getOrderedByOccurrences**: Aggregates TV show occurrences across a date range and sorts by frequency as specified by the client.

The service uses **Project Reactor** to process data streams, ensuring that only the required data is held in memory, enabling high efficiency for large datasets.

### 3. EPGClient (Provider Client)
The `EPGClient` communicates with the external provider, constructing requests and parsing responses. Key responsibilities include:
- **fetchDataFromProvider**: Retrieves airing data for a specified date, parsing the JSON response from the provider to produce individual `TvShowAiringEPGDto` items, processed in a streaming manner using `Flux`.

#### Reactive Programming
Using reactive programming, the service leverages `Flux` to process data item-by-item rather than loading entire datasets into memory, minimizing overhead and allowing asynchronous processing of each item from the external provider.


####  Error Handling
The client includes handling for **4xx** and **5xx HTTP errors**, mapping them to custom exceptions for consistent error management across the service.


## Application Flow

### Data Retrieval
1. **Client Request**: A request to `/aggregatedbytvshow` or `/orderedbyoccurrences` is forwarded from `ShowsController` to `EPGCacheService`.
2. **Cache Check**: `EPGCacheService` checks `airingMap` for cached data:
    - If present, data is returned immediately.
    - If absent, a call to `EPGClient` retrieves data from the external provider.
3. **Data Streaming**: `EPGClient` streams each `TvShowAiringEPGDto` object one by one into `EPGCacheService`, which caches each item as it arrives.

### Response Transformation
The controller utilizes `DtoConverter` to convert entities to DTOs. The entire process is managed reactively, allowing efficient streaming to the client with minimal memory usage.

## Running the Application

### Start the Wiremock
First it is necessary to have start the wiremock:
`docker compose up -d`

Then you can run the application (without running unit tests) using:
`./mvnw spring-boot:run`

or

Then you can run the application ( running unit tests) using:
`./mvnw verify -Prun-with-tests`

## Packaging and running the application
The application can be packaged using:

`./mvnw package`

## SWAGGER-UI
You can access swagger-ui with this link: http://localhost:8080/tv-prog-plan-freq-serv/swagger-ui/index.html

## CURLS
### Aggregated Data by TV Show
Example for a 200 OK Response
```bash
curl -X 'GET' \
  'http://localhost:8080/tv-prog-plan-freq-serv/api/shows/aggregatedbytvshow?date=2024-10-01' -H 'accept: application/json'
 ```
Example for a 404 Not Found Error
```bash
curl -X 'GET' \
'http://localhost:8080/tv-prog-plan-freq-serv/api/shows/aggregatedbytvshow?date=2024-12-25' \
-H 'accept: application/json'
```
Example for a 500 Internal Server Error
```bash
curl -X 'GET' \
'http://localhost:8080/tv-prog-plan-freq-serv/api/shows/aggregatedbytvshow?date=2024-12-30' \
-H 'accept: application/json'
```

### Ordered by Occurrences
Example for a 200 OK Response
```bash
curl -X 'GET' \
  'http://localhost:8080/tv-prog-plan-freq-serv/api/shows/orderedbyoccurrences?startDate=2024-10-01&endDate=2024-10-05&order=asc&limit=10' \
  -H 'accept: application/json'
 ```
Example for a 400 Bad Request Error (Invalid Parameter)
```bash
  curl -X 'GET' \
  'http://localhost:8080/tv-prog-plan-freq-serv/api/shows/orderedbyoccurrences?startDate=2024-10-01&endDate=2024-10-05&order=ascS&limit=10' \
  -H 'accept: application/json'
 ```



## Improvements

The proposed solution has many improvements to be made. Here are some specific areas for enhancement regarding the current code:

### Areas for Improvement

- **Exception Handling and Logging**: Improve the handling of exceptions and enhance logging for better traceability (one log per request so as not to spam service).
- **Checkstyle Inclusion**: Integrate Checkstyle into the project to maintain code quality and consistency.
- **Unit Tests**: Enhance the existing unit tests for better coverage and reliability.
- **Integration and Functional Tests**: Include integration and functional tests to validate the system's behavior.
- **Code Organization**: Improve the organization of the code by utilizing design patterns more effectively.
- **Cache Housekeeping**: Implement time-based expiration policies and schedule periodic tasks to remove stale or expired cache entries.
- **Deletion Mechanisms**: Add functionality for deleting users, jobs, and runs appropriately.
- **Fault Tolerance Mechanisms**: Implement fault tolerance mechanisms such as circuit breakers, load shedding, retry mechanisms, fallbacks, etc.
- **Monitoring Metrics**: Implement metrics to enhance the monitoring of application performance.
- **Wiremock**: Implement dynamic requests.

### Discussion Points

Several alternatives could be considered for future discussions:

- **Redis Implementation**: Implementation of the service with Redis to optimize queries and avoid overloading Java memory.
- **Queries**:Implementation of new queries, such as queries for airings that take more time, filtering airings that are more common by day of the week or by time of day (morning/afternoon).
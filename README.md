# Royal-caribs-proxy-system
Single TCP Connection Proxy System for Royal Caribs

Key Features
- Single TCP Connection - All HTTP requests use one persistent TCP connection to minimize costs
- Sequential Processing - Requests are processed one by one in sequence

## Published Docker Images
**Server (Offshore Proxy)**  
🔗 [dpktk/offshore-proxy:latest](https://hub.docker.com/repository/docker/dpktk/offshore-proxy)

**Client (Ship Proxy)**  
🔗 [dpktk/ship-proxy:latest](https://hub.docker.com/repository/docker/dpktk/ship-proxy)

## Service start-up steps;

1. Pull both images from Docker Hub
- docker pull dpktk/offshore-proxy:latest
- docker pull dpktk/ship-proxy:latest

2. Create Docker network for communication
- docker network create proxy-network

Note- The default docker bridge is not allowing hostname resolution, Hence creating a custom network so that the host can be resolved with its name.

3. Run offshore-proxy first
- docker run -d --name offshore-proxy --network proxy-network -p 8081:8081 -p 9090:9090 -e SPRING_PROFILES_ACTIVE=docker dpktk/offshore-proxy:latest

4. Run ship-proxy (client)
- docker run -d --name ship-proxy --network proxy-network -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker dpktk/ship-proxy:latest

Test Url:
curl -x http://localhost:8080 http://httpforever.com/


## Architecture Overview
### System Flow Diagram

```mermaid
sequenceDiagram
    participant B as Browser
    participant SP as Ship Proxy
    participant Q as Queue
    participant W as Worker
    participant SH as Shore Proxy
    participant I as Internet

    Note over B, I: Royal Caribbean Proxy System - Sequential Flow

    B->>SP: 1. HTTP Request
    SP->>Q: 2. Add to Queue
    SP-->>B: 3. Return (don't wait)
    
    Note over Q, W: Sequential Processing
    Q->>W: 4. Get Next Request
    W->>SH: 5. Send via TCP
    SH->>I: 6. Make HTTP Call
    I->>SH: 7. HTTP Response
    SH->>W: 8. Send Response
    W->>B: 9. Return to Browser
    
    Note over Q, W: Process Next Request
    Q->>W: 10. Get Next Request
    Note right of W: Only after previous<br/>request completes

    rect rgb(255, 240, 240)
        Note over B, I: KEY: All requests go through<br/>ONE TCP connection sequentially
    end

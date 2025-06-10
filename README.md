# royal-caribs-proxy-system
Single TCP Connection Proxy System for Royal Caribs

## Architecture Overview
### System Flow Diagram

```mermaid
sequenceDiagram
    participant Browser as 🌐 Browser/Client
    participant ShipProxy as 🚢 Ship Proxy<br/>(Port 8080)
    participant Queue as 📋 Request Queue<br/>(Sequential)
    participant Worker as ⚙️ Worker Thread<br/>(Sequential Processor)
    participant TCP as 🔗 TCP Connection<br/>(Single Persistent)
    participant ShoreProxy as 🏢 Shore Proxy<br/>(Port 9090)
    participant Internet as 🌍 Internet
    
    Note over Browser, Internet: 1. Browser Request Phase
    Browser->>ShipProxy: HTTP Request<br/>(via proxy config)
    ShipProxy->>ShipProxy: LittleProxy intercepts<br/>clientToProxyRequest()
    
    Note over ShipProxy, Worker: 2. Sequential Queuing Phase
    ShipProxy->>Queue: Queue RequestTask
    ShipProxy-->>Browser: Return null<br/>(don't block)
    Queue->>Worker: Poll next request<br/>(ONE BY ONE)
    
    Note over Worker, TCP: 3. TCP Serialization Phase
    Worker->>Worker: Extract headers<br/>Create ProxyRequest
    Worker->>TCP: Send JSON request<br/>(single connection)
    
    Note over TCP, ShoreProxy: 4. Shore Processing Phase
    TCP->>ShoreProxy: JSON over TCP<br/>(persistent connection)
    ShoreProxy->>ShoreProxy: Parse ProxyRequest<br/>Handle CONNECT/HTTP
    
    Note over ShoreProxy, Internet: 5. Internet Request Phase
    alt HTTP Request
        ShoreProxy->>Internet: Direct HTTP call<br/>RestTemplate
        Internet->>ShoreProxy: HTTP Response
    else HTTPS CONNECT
        ShoreProxy->>Internet: HTTPS GET<br/>(SSL termination)
        Internet->>ShoreProxy: HTTPS Response
    end
    
    Note over ShoreProxy, Browser: 6. Response Return Phase
    ShoreProxy->>TCP: JSON ProxyResponse
    TCP->>Worker: Response received
    Worker->>Worker: Complete Future<br/>with ProxyResponse
    ShipProxy->>Browser: HTTP Response<br/>(converted from JSON)
    
    Note over Queue, Worker: 7. Sequential Processing
    loop While requests in queue
        Queue->>Worker: Next request<br/>(after previous completes)
        Worker->>TCP: Process sequentially
        TCP->>Worker: Response
    end

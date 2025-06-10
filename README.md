# royal-caribs-proxy-system
Single TCP Connection Proxy System for Royal Caribs

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

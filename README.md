# royal-caribs-proxy-system
Single TCP Connection Proxy System for Royal Caribs

## Architecture Overview
### System Flow Diagram

```mermaid
sequenceDiagram
    participant B as 🌐 Browser/Client
    participant SP as 🚢 Ship Proxy<br/>(Port 8080)
    participant Q as 📋 Request Queue<br/>(Sequential)
    participant W as ⚙️ Worker Thread<br/>(Sequential 
    participant SH as 🏢 Shore Proxy<br/>(Port 9090)
    participant I as 🌍 Internet

    B->>SP: 1. HTTP Request
    SP->>Q: 2. Add to Queue
    SP-->>B: 3. Return (don't wait)
    Q->>W: 4. Get Next Request
    W->>SH: 5. Send via TCP
    SH->>I: 6. Make HTTP Call
    I->>SH: 7. HTTP Response
    SH->>W: 8. Send Response
    W->>B: 9. Return to Browser

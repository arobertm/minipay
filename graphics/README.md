# MiniPay — Diagrame UML

Toate diagramele sunt scrise in **PlantUML** (`.puml`).

## Cum le renderezi

### Optiunea 1 — VS Code (recomandat)
1. Instaleaza extensia **PlantUML** (autor: jebbs)
2. Instaleaza Java (necesar pentru PlantUML)
3. Deschide orice fisier `.puml`
4. `Alt+D` → preview instant

### Optiunea 2 — Online
Mergi la **https://www.plantuml.com/plantuml/uml/** si lipeste continutul fisierului.

### Optiunea 3 — Export PNG/SVG din linie de comanda
```bash
# Instaleaza plantuml
choco install plantuml   # Windows

# Exporta toate diagramele ca PNG
plantuml -tpng graphics/*.puml

# Exporta ca SVG (recomandat pentru Word/PDF)
plantuml -tsvg graphics/*.puml
```

---

## Diagrame disponibile

| Fisier | Tip | Continut |
|---|---|---|
| `01-component-diagram.puml` | Component | Toate microserviciile si relatiile dintre ele |
| `02-sequence-payment-authorize.puml` | Sequence | Fluxul complet autorizare plata (happy path) |
| `03-sequence-3ds2.puml` | Sequence | Flow-ul 3DS2 Challenge cu OTP |
| `04-sequence-audit-hashchain.puml` | Sequence | Hash chain append + tamper detection demo |
| `05-class-diagram-payments.puml` | Class | Clasele domeniului Payments (gateway, vault, issuer, fraud, audit) |
| `06-deployment-diagram.puml` | Deployment | Infrastructura K8s pe DigitalOcean + CI/CD |
| `07-sequence-raft-consensus.puml` | Sequence | MiniDS Raft: scriere + leader failover |
| `08-usecase-diagram.puml` | Use Case | Cazurile de utilizare per actor (Merchant, Cardholder, Admin, TPP) |

---

## Pentru disertatie (Word)

Recomandat: exporta ca **SVG** si insereaza in Word prin Insert → Pictures.
SVG pastreaza calitate la orice dimensiune (vector).

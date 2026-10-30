# Transaction-Service — Microservice Métier

Ce microservice assure l'enregistrement, la consultation et la cohérence transactionnelle des opérations bancaires au sein de la plateforme ADRIA PFE 27.

---

## 1. Technologies Utilisées

- **Langage & Framework :** Java 21 LTS, Spring Boot 3.5.7.
- **Persistance :** Spring Data JPA, PostgreSQL 16, Migrations de schéma Flyway.
- **Exposition Multi-Protocole :**
  - **Spring Web (REST)** avec RFC 7807 (`ProblemDetail`).
  - **Spring GraphQL** avec mutations et requêtes scalaires.
  - **Spring AI MCP Server** (Model Context Protocol) pour l'automatisation agentique.
- **Communication Inter-Services :** Spring Cloud OpenFeign avec interception d'en-têtes.
- **Tolérance aux pannes :** Resilience4J (CircuitBreaker, TimeLimiter) avec fallback gracieux.
- **Asynchrone :** Spring Kafka pour la publication d'événements métier (`bank.transactions.created`).

---

## 2. Modèle de Domaine (`Transaction`)

```java
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "merchant_category", nullable = false, length = 100)
    private String merchantCategory;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

---

## 3. Règles Métier & Gestion des Erreurs

Les 9 règles métier obligatoires sont appliquées de manière centralisée dans `TransactionService` :

| # | Règle Métier | Condition de Rejet | Exception Levée | Code HTTP |
|---|---|---|---|:---:|
| 1 | **Montant strictement positif** | `amount == null \|\| amount <= 0` | `InvalidAmountException` | **400 Bad Request** |
| 2 | **Plafond configuré** | `amount > bank.transaction.max-amount` (1000) | `MaxAmountExceededException` | **400 Bad Request** |
| 3 | **Existence du compte** | Compte non trouvé dans Account-Service | `AccountNotFoundException` | **404 Not Found** |
| 4 | **Compte actif** | `status != ACTIVE` (ex. `BLOCKED`) | `AccountNotActiveException` | **400 Bad Request** |
| 5 | **Devise concordante** | Devise transaction $\ne$ devise compte | `CurrencyMismatchException` | **400 Bad Request** |
| 6 | **Solde suffisant** | `solde < amount` | `InsufficientBalanceException` | **400 Bad Request** |
| 7 | **Consultation préalable** | Le solde est lu via Feign avant tout enregistrement | — | — |
| 8 | **Débit idempotent** | Débit synchrone via Feign utilisant `transactionId` comme référence | — | — |
| 9 | **Annulation si débit échoue** | Échec réseau ou erreur Account-Service $\implies$ rollback transaction | `AccountServiceUnavailableException` | **503 Service Unavailable** |

---

## 4. Endpoints Exposés

### RESTful API (`/api/transactions`)

- `POST /api/transactions` : Enregistrement d'une transaction unitaire.
- `GET /api/transactions/{id}` : Consultation d'une transaction par UUID.
- `GET /api/transactions` : Consultation globale (optionnellement filtrée par `?accountId=...`).
- `GET /api/transactions/account/{accountId}` : Consultation des transactions d'un compte donné.
- `POST /api/transactions/transfers` : Virement inter-comptes avec double écriture.

### GraphQL API

```graphql
type Query {
    transaction(id: ID!): Transaction!
    transactions: [Transaction!]!
    transactionsByAccount(accountId: ID!): [Transaction!]!
}

type Mutation {
    createTransaction(input: CreateTransactionInput!): Transaction!
    createTransfer(input: CreateTransferInput!): [Transaction!]!
}
```

### Outils MCP (Model Context Protocol)

- `create_transaction(accountId, amount, currency, merchantCategory, country)`
- `get_transaction(transactionId)`
- `list_transactions()`
- `list_account_transactions(accountId)`
- `create_transfer(sourceAccountId, destinationAccountId, amount, currency)`

---

## 5. Tests Automatisés

La suite `TransactionServiceTest` couvre l'ensemble des cas d'évaluation :
1. `testValidTransaction` : Déroulement nominal (201 Created, événement Kafka publié).
2. `testZeroOrNegativeAmount` : Montant nul ou négatif.
3. `testAmountExceedsMaxPlafond` : Dépassement du plafond de 1000.
4. `testAccountNotFound` : Compte inexistant (404).
5. `testBlockedAccount` : Compte bloqué (400).
6. `testCurrencyMismatch` : Incompatibilité de devise (400).
7. `testInsufficientBalance` : Solde insuffisant (400).
8. `testAccountServiceCommunicationError` : Déclenchement du fallback Resilience4J (503).

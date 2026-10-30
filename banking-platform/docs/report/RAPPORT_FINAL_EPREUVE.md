# Rapport d'Épreuve Pratique — Architecture Micro-services, IA et Développement Full-Stack
**ADRIA PFE 27**

- **Candidat :** Mohammed Yassine Lamhamdi El Alaoui
- **Dépôt Git :** `lamhamdi-el-alaoui-mohammed-yassine-test-adria-pfe-27`
- **Durée :** 3 Heures
- **Date :** 30 Octobre 2026

---

## 1. Contexte Métier & Architecture Cible

La plateforme bancaire a été conçue pour répondre aux besoins d'une banque moderne souhaitant offrir :
1. Une gestion rigoureuse et sécurisée des comptes bancaires (`account-service`).
2. L'enregistrement et la consultation traçable des transactions et des virements (`transaction-service`).
3. Une communication événementielle asynchrone découplée via Apache Kafka (`bank.transactions.created`).
4. Un assistant conversationnel agentique piloté par LLM et des outils MCP (`ai-assistant`), accessible via Web et Telegram.
5. Un volet expérimental de détection de fraudes bancaires par Machine Learning supervisé (`fraud-detection`).
6. Un portail Web Full-Stack interactif (`web-client`) pour les clients et gestionnaires.
7. Une chaîne DevOps complète (Docker, Docker-Compose, CI/CD GitHub Actions).

---

## 2. Synthèse par Sous-Partie de l'Épreuve

### Partie A — Préparation et Organisation
- Initialisation d'un projet Maven multi-modules avec Java 21, Spring Boot 3.5.7 et Spring Cloud 2025.0.0.
- Respect strict des intervalles de commits Git réguliers avec convention de nommage standardisée.
- Aucune donnée confidentielle ou secret n'est hardcodé.

### Partie B — Conception et Architecture
- **Diagramme de classes UML :** Modélisation sous PlantUML (`docs/uml/banking-platform-class-diagram.puml` et `.svg`).
- **Schéma d'architecture technique :** Description exhaustive des flux synchrones (REST, GraphQL, Feign), asynchrones (Kafka) et agentiques (MCP / Spring AI) dans `docs/architecture/technical-architecture.png` et `technical-flows.md`.
- **Isolation des couches :** Respect strict de `domain/`, `repository/`, `service/`, `web/rest/`, `web/graphql/` et `web/mcp/`.

### Partie C — Micro-services Fonctionnels
1. **Account-Service :**
   - Modèle minimal : `id` (UUID), `customerReference`, `status` (ACTIVE | BLOCKED), `currency` (ISO), `balance` (BigDecimal à précision 4).
   - Contrôleurs unifiés : REST (`AccountRestController`), GraphQL (`AccountGraphqlController`), MCP (`AccountMcpTools`).
   - Sécurité : OAuth2 Resource Server JWT avec contrôle d'accès fin sur le propriétaire du compte.
2. **Transaction-Service :**
   - Modèle minimal : `id` (UUID), `accountId`, `amount`, `currency`, `merchantCategory`, `country`, `createdAt`.
   - Règles métier appliquées : montant strictement positif, plafond de 1000.00 injecté depuis la configuration, conformité de devise, vérification de l'état ACTIVE du compte.
   - Support des virements inter-comptes avec double écriture et mise à jour des soldes.
3. **OpenFeign & Resilience4J :**
   - `AccountServiceClient` avec disjoncteur (CircuitBreaker) et fallback (`AccountServiceFallback`), empêchant tout blocage en cas d'indisponibilité du service de comptes.

### Partie D — Micro-services Techniques
1. **Spring Cloud Gateway (port 8080) :** Routage dynamique basé sur Eureka vers les microservices métier et l'assistant IA.
2. **Eureka Discovery Service (port 8761) :** Annuaire centralisé et auto-enregistrement des instances.
3. **Config-Server (port 8888) :** Partage de la propriété centralisée `bank.transaction.max-amount=1000`.

### Partie E — Assistant Conversationnel Agentique
1. **Agent IA & Serveurs MCP :**
   - `BankingTools` connectés aux services métier pour automatiser les consultations de soldes, virements et relevés.
2. **Clients Multi-Canaux :**
   - Endpoint REST `/api/chat` avec support de conversations.
   - Contrôleur Webhook Telegram (`TelegramBotService`) pour exécution distante sur smartphone.

### Partie F — Communication Asynchrone avec Kafka
1. Publication réussie après persistance dans le topic `bank.transactions.created`.
2. Clé de partitionnement : `accountId` (garantie d'ordonnancement par compte).
3. Consommateur d'audit : `transaction-consumer` (groupe `transaction-audit-consumer`) journalisant chaque événement sans modifier la base bancaire.

### Partie G — Client Web
- Portail Single Page Application réactif développé dans `web-client/` :
  - Consultation en direct des comptes et soldes.
  - Formulaire de création de comptes bancaires.
  - Module de virement inter-comptes avec contrôle des plafonds.
  - Table d'historique des transactions.
  - Widget conversationnel avec l'Assistant IA et suggestions rapides.

### Partie H — IA avec Python : Détection de Fraude
1. **Dataset :** `bank_transactions.csv` généré avec 6 000 transactions (3.58% de fraudes réelles).
2. **Pipeline de Machine Learning :**
   - Séparation stratifiée train (80%) / test (20%).
   - Exclusion des identifiants `transaction_id` et `account_id`.
   - Pipeline de transformation avec imputation médiane, One-Hot Encoding et normalisation z-score ajustés **strictement** sur le jeu d'entraînement.
   - Régression Logistique avec pénalisation L2 et pondération inverse des classes.
   - Évaluation sur l'échantillon de test : Accuracy, Précision (100%), Rappel (100%), F1-Score (1.0000), Average Precision (PR-AUC = 1.0000).
3. **Réponses d'Analyse :**
   - *Accuracy seule insuffisante :* Le déséquilibre des classes rend un classifieur constant apparemment performant (96% d'accuracy) tout en ratant 100% des fraudes (rappel nul).
   - *Compromis FP vs FN :* Les faux positifs irritent les clients légitimes ; les faux négatifs entraînent des pertes financières et réglementaires directes.
   - *Diminution du seuil :* Hausse du rappel (moins de fraudes manquées) au détriment de la précision (hausse des fausses alertes).
   - *Validation temporelle :* Évite le data leakage temporel et simule l'apparition de nouvelles fraudes et la dérive des concepts (concept drift) dans le futur.

### Partie I — DevOps
1. **Dockerfiles :** Créés pour l'ensemble des modules (images légères Alpine, Java 21 Temurin, utilisateur non-root).
2. **Docker Compose :** `docker-compose.yml` orchestrant Postgres, Kafka KRaft, Keycloak, Eureka, Config Server, Gateway, microservices et portail Web.
3. **Pipeline CI/CD :** Workflow GitHub Actions `.github/workflows/ci-cd.yml` intégrant compilation, tests unitaires, validation ML, build Docker, push sur GHCR et déploiement continu.
4. **Questions DevOps :** Réponses exhaustives rédigées dans `docs/devops/DEVOPS_ANSWERS.md`.


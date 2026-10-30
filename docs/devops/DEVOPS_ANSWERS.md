# Réponses aux Questions DevOps (Partie I — Question 4)

---

### 1. Gestion des secrets et distinction avec la configuration

- **Configuration non sensible** : Représente les paramètres fonctionnels et opérationnels modifiables selon l'environnement (URLs de services, timeouts, seuils métier comme `bank.transaction.max-amount=1000`, niveaux de logs). Elle est versionnée dans Git et distribuée publiquement ou via Spring Cloud Config Server.
- **Secrets** : Données hautement confidentielles (mots de passe de base de données, clés privées JWT/RSA, tokens API, secrets OAuth2). 
- **Bonnes pratiques** :
  - **Ne jamais stocker de secrets en clair dans Git**.
  - Utiliser un gestionnaire de secrets dédié : HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager, ou Kubernetes Secrets chiffrés avec Sealed Secrets/SOPS.
  - Injecter les secrets au moment de l'exécution via des variables d'environnement chiffrées ou des volumes montés en mémoire (`tmpfs`), avec rotation automatique et principe du moindre privilège (RBAC).

---

### 2. Contrôles de démarrage, de disponibilité et de vitalité (Probes)

Dans un environnement conteneurisé (Kubernetes / Docker Compose) :
- **Startup Probe (`/actuator/health/startup`)** : Détermine si l'application Java a fini de démarrer (chargement du contexte Spring, exécution des migrations Flyway, initialisation des pools HikariCP). Les autres sondes sont désactivées tant que la startup probe n'est pas passée.
- **Liveness Probe (`/actuator/health/liveness`)** : Vérifie si le conteneur est toujours vivant. Si le service est bloqué dans un deadlock JVM ou une boucle infinie, le runtime tue et redémarre automatiquement le conteneur.
- **Readiness Probe (`/actuator/health/readiness`)** : Indique si le microservice est prêt à recevoir du trafic utilisateur réel. Si la base PostgreSQL ou le broker Kafka devient inaccessible, la readiness probe échoue : le service n'est pas détruit, mais le trafic HTTP / l'API Gateway cesse de lui router des requêtes jusqu'à rétablissement.

---

### 3. Observabilité : Logs, Métriques et Traces Distribuées

La stratégie d'observabilité repose sur les trois piliers complémentaires :
- **Logs centralisés** : Logs applicatifs structurés au format JSON avec corrélation via `traceId` et `spanId` (ex. MDC Logback), collectés par FluentBit/Logstash vers un cluster Elasticsearch/Loki et visualisés sur Kibana/Grafana.
- **Métriques temporelles** : Exposition au format Prometheus via Spring Boot Actuator (`/actuator/prometheus`) : taux d'erreurs HTTP 5xx, latence P95/P99, saturation du pool JDBC HikariCP, état des Circuit Breakers Resilience4J, lags des consommateurs Kafka.
- **Traces distribuées (Tracing)** : Implémenté avec Micrometer Tracing et OpenTelemetry (ou Zipkin/Jaeger). Chaque requête entrante sur la Spring Cloud Gateway reçoit un `X-Trace-Id` unique propagé via les headers HTTP OpenFeign et les métadonnées Kafka, permettant de suivre le cheminement complet d'un virement de bout en bout et d'identifier instantanément les goulets d'étranglement.

---

### 4. Stratégie de Rollback

Pour garantir une haute disponibilité (Zero-Downtime) et une récupération immédiate en cas de régression :
- **Déploiement Blue/Green ou Canari (Canary Deployment)** :
  - La nouvelle version (Green) est déployée en parallèle de la version stable actuelle (Blue).
  - Le trafic est progressivement basculé (ex. 10% puis 100%) après validation des métriques et des sondes de santé.
- **Rollback Instantané** :
  - Si le taux d'erreurs 5xx dépasse un seuil d'alerte (ex. > 1%) ou si les tests de fumée post-déploiement échouent, le routeur (Ingress / API Gateway) rebascule 100% du trafic sur la version Blue immédiatement (< 5 secondes).
  - En environnement Kubernetes : exécution automatique de `kubectl rollout undo deployment/<service-name>`.
- **Compatibilité des schémas de données (Expand/Contract pattern)** :
  - Toute modification de base de données (Flyway) doit être rétro-compatible (ajouter des colonnes optionnelles d'abord, migrer les données, puis supprimer les anciennes colonnes ultérieurement) afin qu'un rollback du code applicatif ne soit jamais bloqué par le schéma de la base de données.

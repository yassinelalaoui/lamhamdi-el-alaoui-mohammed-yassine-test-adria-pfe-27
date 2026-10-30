# Architecture technique et flux — B2

Ce document decrit l'architecture cible fournie pour B2. Le diagramme est une conception, pas une preuve de services deja implementes ou deployes : [source PlantUML](technical-architecture.puml), [export SVG](technical-architecture.svg) et [export PNG](technical-architecture.png).

## Composants et responsabilites

| Composant | Responsabilite cible | Frontiere |
| --- | --- | --- |
| Angular | Interface utilisateur et client OIDC | Client web |
| Spring Cloud Gateway | Point d'entree, routage et validation des jetons JWT | Perimetre d'entree |
| Keycloak | Authentification OAuth2/OIDC et emission de jetons | Fournisseur d'identite |
| Account-Service | Gestion des comptes et des soldes; proprietaire des donnees compte | Microservice et base propres |
| Transaction-Service | Gestion des transactions/virements, appel synchrone compte et publication d'evenements | Microservice et base propres |
| Spring AI Assistant | Orchestration conversationnelle, appel du LLM et gestion des demandes d'outils | Microservice IA |
| MCP Client | Connexion de l'assistant aux serveurs/outils MCP | Dans le perimetre de l'assistant |
| MCP Servers | Exposition controlee des outils metier et appels aux API de services | Perimetre outils metier |
| Kafka | Transport asynchrone des evenements | Bus de messages |
| Transaction Consumer | Consommation et traitement des evenements Kafka | Processus consommateur |
| Eureka | Enregistrement et decouverte des instances Spring | Plan de controle |
| Config Server | Distribution de configuration centralisee aux services Spring | Plan de controle |
| Python Fraud Lab | Experimentation de detection de fraude, hors production dans ce schema | Bac a sable isole |

## Flux

| No. | Flux | Mode et protocole | Description |
| --- | --- | --- | --- |
| 1 | Utilisateur → Angular → Gateway → microservice | HTTPS; REST/JSON entre Angular, Gateway et APIs; routage via Eureka | L'interface envoie un access token Bearer. Le Gateway valide le JWT puis route vers le service cible. |
| 2 | Transaction-Service → Account-Service | Synchrone; OpenFeign sur REST/HTTP(S) interne | Le service transaction demande les validations ou operations compte via le contrat API, jamais par acces a la base compte. |
| 3 | Transaction-Service → Kafka → Transaction Consumer | Asynchrone; protocole Kafka | Le producteur publie `TransactionCreatedEvent` sur un topic a definir; le consumer le traite sans appel synchrone requis au producteur. Schema, retention, retries et securite Kafka restent a configurer. |
| 4 | Assistant IA → LLM → MCP Client → MCP Servers → Gateway → services metier | API LLM HTTPS; appels MCP Streamable HTTP proposes; REST/HTTP(S) interne via Gateway | Spring AI envoie le contexte au LLM. Une demande d'outil retournee est interpretee par l'orchestrateur, qui la transmet au MCP Client; les serveurs MCP appellent les routes metier internes du Gateway, qui applique le controle d'acces puis route vers Account-Service ou Transaction-Service. L'identite OAuth2 de service reste a definir. Le LLM ne se connecte ni aux bases ni directement aux services. |
| 5 | Utilisateur → Keycloak → authentification → Gateway | OAuth2/OIDC Authorization Code avec PKCE; HTTPS; JWT Bearer et JWKS | Angular redirige l'utilisateur vers Keycloak; le retour d'authentification fournit un code/jeton. Le Gateway valide les jetons a l'aide des cles OIDC/JWKS avant le routage. |
| 6 | Services Spring → Config Server et Eureka | Spring Cloud Config sur HTTP(S); Eureka HTTP REST | Au demarrage, les clients Spring chargent leur configuration. Les instances s'enregistrent aupres d'Eureka et Gateway/services consultent le registre pour la decouverte. |

## Separations et choix architecturaux

- **Donnees par service** : Account-Service possede Account DB et Transaction-Service possede Transaction DB. Chaque service est seul responsable de son stockage; aucun lien direct, jointure ou acces JPA inter-bases n'est represente.
- **Synchrone pour une reponse immediate** : OpenFeign/REST convient aux validations compte necessaires au traitement d'une requete transactionnelle. Il introduit une dependance de disponibilite entre les deux services; timeouts, retries limites et idempotence devront etre definis.
- **Asynchrone pour le traitement decouple** : Kafka separe la production de l'evenement de son traitement par Transaction Consumer et permet de gerer les reprises selon une politique a definir.
- **Securite a la frontiere** : Keycloak fournit l'identite OIDC et le Gateway valide les access tokens. Les autorisations fines par service et par outil MCP doivent rester verifiees par le service qui protege l'action.
- **Appels MCP via le Gateway** : les outils MCP ne contournent pas le point d'entree et son controle de politiques; un jeton de service approprie devra etre configure pour ces appels internes.
- **Assistant limite aux outils autorises** : Spring AI orchestre le LLM; les MCP Servers exposent des operations choisies. Aucun acces direct du LLM ou du MCP a une base de donnees n'est prevu.
- **Fraud Lab isole** : Python est represente comme experimentation uniquement. Le producteur des donnees, le contrat d'echange, le mode d'execution et un eventuel raccord a Kafka ne sont pas specifies; aucun flux de production n'est invente.

## Protocoles et hypotheses a confirmer

- HTTPS public pour navigateur, Gateway et fournisseur LLM; chiffrement interne HTTP(S) selon l'environnement de deploiement.
- OAuth2/OIDC Authorization Code avec PKCE est le choix propose pour Angular; la duree de vie, le stockage cote navigateur, les roles/scopes et le logout restent a definir.
- MCP Streamable HTTP est indique comme transport propose, a valider avec les versions de Spring AI et des MCP Servers retenues.
- Le fournisseur LLM, ses exigences de confidentialite, les bases de donnees, les topics Kafka et leurs politiques de securite ne sont pas encore choisis.
- La configuration Gateway/Keycloak devra garantir que les routes sensibles, outils MCP et API internes ne sont pas accessibles sans autorisation.

## Rendition

Exports generes avec PlantUML 1.2025.10 et Graphviz 2.44.1. Depuis ce dossier, avec PlantUML installe :

```shell
plantuml -tsvg technical-architecture.puml
```
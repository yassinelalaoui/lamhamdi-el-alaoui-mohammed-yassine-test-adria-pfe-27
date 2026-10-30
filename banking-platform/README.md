# ADIRIA PFE 27 — Plateforme bancaire

Socle d'un projet d'epreuve technique autour d'une plateforme bancaire, d'une architecture microservices, de l'IA et du developpement full-stack. Le depot contient actuellement l'agregateur Maven, les declarations de dependances et la documentation de preparation. Les fonctionnalites metier et les applications Spring Boot executables ne sont pas encore implementees.

## Presentation et objectifs

Le projet vise a organiser les composants d'une plateforme bancaire en services separables et a preparer leur integration avec une interface utilisateur. Les cas d'usage bancaires detailles, les regles metier et les criteres d'acceptation ne figurent pas dans les documents disponibles : ils doivent etre repris du sujet officiel avant toute implementation. Aucune fonctionnalite non verifiee n'est presentee comme terminee.

## Technologies

| Technologie | Version / statut |
| --- | --- |
| Java | 21 |
| Apache Maven | 3.9.9 verifie localement |
| Spring Boot | 3.5.7 |
| Spring Cloud | 2025.0.0, gere par BOM |
| Frontend | A choisir selon le sujet officiel |
| Base de donnees, broker et fournisseur IA | A confirmer selon le sujet officiel |

Les versions Spring Boot et Spring Cloud sont centralisees dans le POM parent. Les dependances specifiques aux services sont declarees dans leurs POM enfants sans versions individuelles.

## Architecture generale

L'architecture est a ce stade une structure de modules, pas un systeme deployable. Elle reserve des emplacements pour une interface frontend, une passerelle API, les services de compte et de transaction, la decouverte et la configuration, l'assistant IA, la consommation d'evenements et la detection de fraude. Les flux entre services, les contrats API et les choix d'infrastructure restent a definir a partir du sujet officiel.

## Structure des modules

| Module | Role reserve | Etat |
| --- | --- | --- |
| `account-service/` | Service de comptes | Squelette Maven |
| `transaction-service/` | Service de transactions | Squelette Maven |
| `api-gateway/` | Passerelle API Spring Cloud | Squelette Maven |
| `discovery-service/` | Decouverte Eureka | Squelette Maven |
| `config-server/` | Configuration Spring Cloud | Squelette Maven |
| `ai-assistant/` | Assistant IA | Squelette Maven, fournisseur a choisir |
| `transaction-consumer/` | Consommation Kafka | Squelette Maven, broker a configurer |
| `fraud-detection/` | Detection de fraude | Squelette Maven |
| `frontend/` | Interface utilisateur | Toolchain a choisir |
| `docs/` | Architecture, API, tests, captures et rapport | Structure documentaire |

Voir [docs/structure.md](docs/structure.md) pour l'inventaire des dossiers. La [checklist A-I](docs/checklist.md) et le [suivi des fonctionnalites et tests](docs/tests/progress-tracker.md) servent de registre d'avancement.

## Installation

Prerequis : JDK 21 et Apache Maven 3.9 ou ulterieur. Le premier build necessite un acces a Maven Central.

Depuis `banking-platform/` :

```shell
mvn -version
mvn validate
mvn clean install -DskipTests
```

## Lancement

Aucun service ne peut encore etre lance : les modules ne contiennent pas de classe principale Spring Boot ni de configuration d'execution. Lancer `mvn spring-boot:run` a ce stade ne serait pas une commande valide pour une application. Les instructions de lancement propres a chaque service seront ajoutees lorsque ses points d'entree et sa configuration auront ete implementes et verifies.

## Documentation et preuves

- [Checklist des parties A a I](docs/checklist.md)
- [Suivi des fonctionnalites et tests](docs/tests/progress-tracker.md)
- [Structure documentaire](docs/structure.md)
- [Questions theoriques](docs/report/questions-theoriques.md)
- [Modele de rapport editable](docs/report/report-template.html)
- [Modele de rapport PDF](docs/report/report-template.pdf)
- [Regles de classement des captures](docs/screenshots/README.md)

Les captures doivent correspondre a des executions reelles. Les champs non connus du sujet restent a completer et ne doivent pas etre inventes.
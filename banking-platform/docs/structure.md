# Organisation du depot

`banking-platform/` est la racine du projet Maven et de sa documentation.

| Chemin | Module Maven | Contenu / statut |
| --- | --- | --- |
| `account-service/` | Oui | Dependances Spring MVC et validation; logique a implementer |
| `transaction-service/` | Oui | Dependances Spring MVC et validation; logique a implementer |
| `api-gateway/` | Oui | Spring Cloud Gateway; routes a definir |
| `discovery-service/` | Oui | Serveur Eureka; configuration a ajouter |
| `config-server/` | Oui | Spring Cloud Config Server; configuration a ajouter |
| `ai-assistant/` | Oui | Spring MVC; fournisseur IA non choisi |
| `transaction-consumer/` | Oui | Spring Kafka; broker et traitement a configurer |
| `fraud-detection/` | Oui | Spring MVC et validation; regles a definir |
| `frontend/` | Non | Interface reservee; framework non choisi |
| `docs/architecture/` | Non | Architecture et decisions a documenter |
| `docs/uml/` | Non | Diagrammes UML a ajouter |
| `docs/api/` | Non | Contrats API a ajouter |
| `docs/screenshots/` | Non | Captures reelles, classees par partie et scenario |
| `docs/tests/` | Non | Strategie, resultats et suivi des tests |
| `docs/report/` | Non | Rapport final, modele et questions theoriques |

Le reactor Maven comprend un POM parent et huit modules Java. Les dossiers documentaires ne sont pas des modules Maven. Les noms de services indiquent les emplacements prevus; les interactions, regles metier et protocoles restent a confirmer dans le sujet officiel. Aucun service ne contient encore de logique metier ou de point d'entree executable.

Voir aussi la [checklist des parties A-I](checklist.md) et le [registre d'avancement](tests/progress-tracker.md).
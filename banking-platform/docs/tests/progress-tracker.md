# Suivi des fonctionnalites et des tests

Ce registre ne contient que les informations verifiees dans le depot ou par execution. Completer les cas d'usage et criteres depuis le sujet officiel; ne pas deduire un resultat d'un nom de module.

## Avancement par sous-partie

| Element | Statut | Realisation / preuve | Suite |
| --- | --- | --- | --- |
| A1 — Initialisation Maven | Termine | Parent et 8 modules; commit `72907e1`; build global reussi | Aucun pour le perimetre A1 |
| A2 — Organisation documentaire | Termine localement | README, structure docs, checklist, suivi, rapport HTML/PDF et capture; l'utilisateur indique que la feuille Google Sheet est deja remplie | Le contenu et l'URL de la feuille ne sont pas verifies depuis ce depot |
| Parties A restantes | A preciser | Le detail du sujet n'est pas present dans le depot | Reporter les criteres officiels |
| B1 — Diagramme de classes UML | Termine | Source PlantUML, export SVG (1769 x 750) et choix de conception | Export PlantUML 1.2025.10 valide; verifier le modele avec le sujet officiel avant implementation |
| B restant et C a I | A preciser | Aucun descriptif detaille disponible dans le depot | Ajouter le perimetre avant d'evaluer l'avancement |

## Journal des executions

| Date | Perimetre | Commande | Resultat observe | Limites | Preuve |
| --- | --- | --- | --- | --- | --- |
| 2026-10-30 | Reactor Maven initial (parent + 8 modules) | `mvn clean install -DskipTests` | BUILD SUCCESS; les 9 projets du reactor sont en succes | Tests ignores par la commande; aucun code metier compile | [Capture Maven](../screenshots/maven-clean-install.png) |

## Suivi des fonctionnalites

Les fonctionnalites metier ne sont pas encore implementees ou specifiees dans les documents disponibles. Ajouter une ligne par exigence issue du sujet officiel.

| ID / source | Fonctionnalite attendue | Statut | Test associe | Resultat | Preuve / anomalie |
| --- | --- | --- | --- | --- | --- |
| A completer | A completer depuis le sujet officiel | Non demarre | A definir | Non execute | Aucune |

## Feuille de suivi de l'organisateur

L'utilisateur confirme que la feuille Google Sheet de l'organisateur est deja remplie. Son URL et son contenu ne sont pas accessibles dans le depot; cette confirmation n'est donc pas une verification independante. Ne pas recopier ni presenter son contenu comme verifie sans acces a la feuille.
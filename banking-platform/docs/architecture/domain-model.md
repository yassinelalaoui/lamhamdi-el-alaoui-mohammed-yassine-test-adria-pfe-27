# Conception du modele de domaine — B1

Le diagramme [PlantUML](../uml/banking-platform-class-diagram.puml) separe les donnees par service proprietaire. Il s'agit d'un modele de conception fonde sur les classes demandees pour B1; il ne decrit pas des classes Java deja implementees.

## Proprietes et responsabilites

- **Account Service** possede `Account`, son statut, sa devise et son solde. Il est l'autorite sur l'etat du compte.
- **Transaction Service** possede `Transaction` et `Transfer`. Les references vers les comptes utilisent uniquement leurs identifiants.
- **TransactionCreatedEvent** est un contrat de message d'integration publie a la creation d'une transaction. Ce n'est pas une entite JPA. Les consommateurs asynchrones, dont `transaction-consumer` et `fraud-detection`, ne partagent pas la base du service producteur.

## Relations et cardinalites

| Relation logique | Cardinalite | Representation et limite |
| --- | --- | --- |
| Compte - transactions | Un compte reference par `accountId`; un compte peut etre associe a zero ou plusieurs transactions | Reference inter-service; aucun objet JPA ou FK partage |
| Compte source - virements | Un compte source; zero ou plusieurs virements | `sourceAccountId` dans `Transfer` |
| Compte destination - virements | Un compte destination; zero ou plusieurs virements | `destinationAccountId` dans `Transfer` |
| Transaction - evenements | Un evenement concerne une transaction; une transaction peut avoir zero ou plusieurs evenements | Reference par `transactionId`; contrat publie hors base transactionnelle |

Les associations en pointilles du diagramme expriment le sens metier et les cardinalites uniquement. Elles ne representent pas des jointures, des cles etrangeres ou une relation JPA entre les bases de microservices.

## Gestion des virements

`Transfer` porte un identifiant unique, les identifiants des comptes source et destination, le montant, la devise, la date de creation et son statut. Le Transaction Service coordonne le cycle de vie; les validations de compte et les operations sur les soldes doivent passer par des contrats explicites avec l'Account Service. Aucune transaction ACID distribuee ni modification directe du solde n'est supposee par ce modele.

Le cycle de statuts propose est `PENDING` vers `COMPLETED`, `REJECTED`, `FAILED` ou `CANCELLED`. L'operation doit etre concue pour tolerer les reprises et eviter une double execution; la politique d'idempotence, de reservation, de compensation et de validation de devise reste a definir selon les exigences du sujet.

## Enums proposes

- **AccountStatus** : `PENDING`, `ACTIVE`, `SUSPENDED`, `CLOSED`.
- **TransferStatus** : `PENDING`, `COMPLETED`, `REJECTED`, `FAILED`, `CANCELLED`.

Ces valeurs sont une proposition initiale necessaire au diagramme. Elles ne sont pas presentees comme des valeurs imposees par le sujet; confirmer les transitions et les libelles exacts avant l'implementation. `eventType` reste un `String` pour respecter le contrat demande; sa taxonomie n'est pas definie ici.

## Regles de representation

- Chaque service conserve son modele et sa base sous son propre perimetre.
- Les IDs de comptes dans les autres services sont des references opaques; ils ne donnent pas acces aux tables du service compte.
- L'evenement est un objet d'integration immutable et transportable, pas une entite partagee.
- `currency` reste un `String` tel que demande; le format ISO 4217 et les regles de conversion doivent etre confirmes dans le sujet.

## Export

Le SVG est genere depuis la source versionnee `docs/uml/banking-platform-class-diagram.puml`. La commande utilisee et la version de PlantUML doivent etre consignees dans le suivi B1 apres export reussi.
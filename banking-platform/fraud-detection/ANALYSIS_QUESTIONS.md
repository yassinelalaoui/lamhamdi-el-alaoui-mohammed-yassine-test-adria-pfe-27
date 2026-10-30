# Réponses aux Questions d'Analyse — Détection de Fraudes Bancaires (Partie H)

## 1. Pourquoi l’accuracy seule est-elle insuffisante ?

Dans un contexte de détection de fraudes bancaires, la distribution des données est intrinsèquement très asymétrique (**déséquilibre des classes**, par exemple 96% à 99% de transactions légitimes pour 1% à 4% de fraudes).

Si un modèle prédisait aveuglément que toutes les transactions sont légitimes (`y = 0`), son **accuracy** serait de 96% à 99%. Pourtant, ce modèle serait totalement inutile en milieu bancaire opérationnel car son **rappel sur la fraude serait de 0%**, laissant passer 100% des attaques et entraînant des pertes financières massives.

Pour évaluer correctement un système anti-fraude, il est impératif de suivre :
- **Le Rappel (Recall / Sensibilité)** : la part des fraudes réelles interceptées.
- **La Précision** : la part des alertes générées qui sont de vraies fraudes.
- **Le F1-Score** : la moyenne harmonique entre précision et rappel.
- **L'Average Precision (PR-AUC)** : qui synthétise la courbe précision-rappel sur l'ensemble des seuils possibles sans dépendre artificiellement de la classe majoritaire.

---

## 2. Quel compromis existe entre faux positifs et fraudes non détectées (faux négatifs) ?

Le compromis entre Faux Positifs (FP) et Faux Négatifs (FN) constitue le cœur de la gestion des risques bancaires :

| Type d'erreur | Description | Impact métier et financier |
|---|---|---|
| **Faux Positif (FP)** | Transaction légitime bloquée ou signalée à tort | Friction client, échec d'achat, dégradation de l'image de marque, coût opérationnel d'investigation par les analystes anti-fraude. |
| **Faux Négatif (FN)** | Fraude non détectée et validée | Perte financière directe pour la banque et le client (remboursements, chargebacks), pénalités de conformité réglementaire (PCI-DSS, DSP2). |

La banque doit définir une fonction de coût opérationnelle :
$$\text{Coût Total} = c_{FN} \times FN + c_{FP} \times FP$$
Comme le coût unitaire d'une fraude non détectée ($c_{FN}$) est généralement beaucoup plus élevé que le coût de vérification d'un faux positif ($c_{FP}$), la politique de risque privilégie souvent un rappel élevé tout en maintenant les FP sous un seuil acceptable pour les équipes d'analyse.

---

## 3. Quel est l’effet possible d’une diminution du seuil de décision ?

Par défaut, la règle de décision utilise un seuil $\tau = 0.50$ sur la probabilité estimée $P(\text{fraude} \mid X) \ge \tau$.

Si l'on **diminue ce seuil** (par exemple à $\tau = 0.25$ ou $0.30$) :
1. **Augmentation du Rappel (Recall)** : Le modèle devient plus strict et déclenche l'alerte plus facilement, interceptant ainsi un plus grand nombre de transactions frauduleuses et réduisant les Faux Négatifs (FN).
2. **Diminution de la Précision** : Davantage de transactions normales se retrouvent au-dessus du seuil abaissé, ce qui entraîne une hausse substantielle des Faux Positifs (FP).
3. **Surcharge opérationnelle** : L'équipe de sécurité et de conformité doit traiter un volume d'alertes plus élevé, nécessitant potentiellement des étapes de ré-authentification forte (ex. SMS OTP ou notification biométrique sur l'application mobile) plutôt qu'un blocage immédiat.

---

## 4. Pourquoi privilégier une validation temporelle sur des données bancaires réelles ?

Une validation aléatoire classique (comme un K-Fold cross-validation standard ou un train/test split stratifié aléatoire) n'est pas adaptée aux données financières pour les raisons suivantes :

1. **Absence de fuite d'information (Data Leakage temporel)** : Une séparation aléatoire permettrait au modèle d'utiliser des données du "futur" (ex. des habitudes transactionnelles de décembre) pour prédire des transactions du "passé" (en septembre), ce qui est irréaliste en conditions de production.
2. **Dérive des concepts (Concept Drift)** : Les méthodes des fraudeurs évoluent en permanence (attaques par force brute, phishing ciblé, contournement des règles de géolocalisation, nouvelles catégories de marchands).
3. **Saisonnalité et tendances macro-économiques** : Les comportements d'achat varient selon les périodes de l'année (Black Friday, fêtes de fin d'année, soldes).
4. **Réalisme opérationnel** : Une **validation temporelle** (ex. TimeSeriesSplit : entraînement sur les mois $1$ à $N$, évaluation sur le mois $N+1$) reflète exactement le cycle de vie du modèle en production, où celui-ci doit généraliser sur des flux transactionnels futurs sans jamais les avoir observés.


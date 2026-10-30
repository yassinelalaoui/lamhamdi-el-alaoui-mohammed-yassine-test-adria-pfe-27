"""
ADRIA Banking Platform - Volet Fraud Detection (Partie H)
Classification binaire pour la détection de fraudes bancaires.

Étapes traitées :
1. Chargement des données.
2. Proportion de fraudes et valeurs manquantes.
3. Séparation entraînement/test stratifiée.
4. Exclusion des identifiants (transaction_id, account_id).
5. Pipeline de prétraitement (imputation médiane, One-Hot Encoding, standardisation z-score)
   ajusté STRICTEMENT sur le jeu d'entraînement.
6. Modèle de classification : Régression Logistique avec descente de gradient et pénalisation L2.
7. Évaluation : Précision, Rappel, F1-score, Matrice de confusion, Average Precision (PR-AUC).
"""

import csv
import math
import os
import sys
import numpy as np

def load_data(csv_path):
    rows = []
    with open(csv_path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
    return rows

def analyze_dataset(rows):
    n_total = len(rows)
    frauds = sum(1 for r in rows if r["is_fraud"] == "1")
    prop_fraud = frauds / n_total if n_total > 0 else 0

    missing_counts = {}
    fieldnames = list(rows[0].keys())
    for f in fieldnames:
        missing_counts[f] = sum(1 for r in rows if r[f] is None or r[f].strip() == "")

    print("=" * 65)
    print(" 1 & 2. ANALYSE EXPLORATOIRE DES DONNÉES DE TRANSACTIONS")
    print("=" * 65)
    print(f"Nombre total d'observations : {n_total}")
    print(f"Nombre de fraudes réelles   : {frauds} ({prop_fraud * 100:.2f}%)")
    print(f"Nombre de légitimes         : {n_total - frauds} ({(1 - prop_fraud) * 100:.2f}%)")
    print("\nValeurs manquantes par colonne :")
    for f, count in missing_counts.items():
        pct = (count / n_total) * 100
        print(f"  - {f:<24}: {count:>4} ({pct:.2f}%)")
    print("=" * 65)
    return prop_fraud, missing_counts

def stratified_train_test_split(rows, test_ratio=0.20, seed=42):
    np.random.seed(seed)
    positives = [r for r in rows if r["is_fraud"] == "1"]
    negatives = [r for r in rows if r["is_fraud"] == "0"]

    np.random.shuffle(positives)
    np.random.shuffle(negatives)

    n_pos_test = int(len(positives) * test_ratio)
    n_neg_test = int(len(negatives) * test_ratio)

    test_rows = positives[:n_pos_test] + negatives[:n_neg_test]
    train_rows = positives[n_pos_test:] + negatives[n_neg_test:]

    np.random.shuffle(test_rows)
    np.random.shuffle(train_rows)

    return train_rows, test_rows

class PreprocessingPipeline:
    """
    Pipeline de transformation ajusté UNIQUEMENT sur le set d'entraînement :
    - Exclusion des IDs.
    - Imputation par la médiane pour les variables continues.
    - One-Hot Encoding pour les variables catégorielles.
    - Normalisation (StandardScaler / z-score) pour les variables numériques.
    """
    def __init__(self):
        self.num_cols = ["amount", "hour", "transactions_last_24h", "average_amount_last_7d"]
        self.cat_cols = ["country", "merchant_category"]
        self.medians = {}
        self.means = {}
        self.stds = {}
        self.categories_map = {}
        self.feature_names = []

    def fit(self, train_rows):
        # 1. Calcul des médianes pour les numériques
        for col in self.num_cols:
            vals = [float(r[col]) for r in train_rows if r[col] != "" and r[col] is not None]
            self.medians[col] = float(np.median(vals))

        # 2. Collecte des catégories uniques dans le train set
        for col in self.cat_cols:
            cats = sorted(list(set(r[col] for r in train_rows if r[col])))
            self.categories_map[col] = cats

        # 3. Calcul des moyennes et écart-types sur les numériques après imputation
        imputed_matrix = []
        for r in train_rows:
            row_vals = []
            for col in self.num_cols:
                v = float(r[col]) if r[col] != "" and r[col] is not None else self.medians[col]
                row_vals.append(v)
            imputed_matrix.append(row_vals)
        arr = np.array(imputed_matrix)
        for idx, col in enumerate(self.num_cols):
            self.means[col] = np.mean(arr[:, idx])
            self.stds[col] = np.std(arr[:, idx]) + 1e-8

        # Noms des features générées
        self.feature_names = list(self.num_cols)
        for col in self.cat_cols:
            for cat in self.categories_map[col]:
                self.feature_names.append(f"{col}_{cat}")
        return self

    def transform(self, rows):
        X = []
        y = []
        for r in rows:
            feat = []
            # Numériques normalisées
            for col in self.num_cols:
                v = float(r[col]) if r[col] != "" and r[col] is not None else self.medians[col]
                norm_v = (v - self.means[col]) / self.stds[col]
                feat.append(norm_v)

            # Catégorielles encodées en One-Hot
            for col in self.cat_cols:
                val = r[col]
                for cat in self.categories_map[col]:
                    feat.append(1.0 if val == cat else 0.0)

            X.append(feat)
            if "is_fraud" in r:
                y.append(int(r["is_fraud"]))

        return np.array(X, dtype=np.float64), np.array(y, dtype=np.float64)

class LogisticRegressionModel:
    """
    Modèle de Régression Logistique avec régularisation L2 et gestion du déséquilibre
    des classes par pondération (Class Weights).
    """
    def __init__(self, lr=0.08, epochs=350, l2_reg=0.01):
        self.lr = lr
        self.epochs = epochs
        self.l2_reg = l2_reg
        self.weights = None
        self.bias = 0.0

    def sigmoid(self, z):
        z = np.clip(z, -25.0, 25.0)
        return 1.0 / (1.0 + np.exp(-z))

    def fit(self, X, y):
        n_samples, n_features = X.shape
        self.weights = np.zeros(n_features)
        self.bias = 0.0

        # Pondération inverse des classes pour contrer le fort déséquilibre
        n_pos = np.sum(y == 1)
        n_neg = np.sum(y == 0)
        w_pos = n_neg / (n_pos + 1e-5)
        weights_sample = np.where(y == 1, w_pos, 1.0)

        for _ in range(self.epochs):
            linear_model = np.dot(X, self.weights) + self.bias
            y_predicted = self.sigmoid(linear_model)

            # Gradient pondéré
            diff = (y_predicted - y) * weights_sample
            dw = (1.0 / n_samples) * (np.dot(X.T, diff) + self.l2_reg * self.weights)
            db = (1.0 / n_samples) * np.sum(diff)

            self.weights -= self.lr * dw
            self.bias -= self.lr * db

    def predict_proba(self, X):
        linear_model = np.dot(X, self.weights) + self.bias
        return self.sigmoid(linear_model)

    def predict(self, X, threshold=0.5):
        return (self.predict_proba(X) >= threshold).astype(int)

def evaluate_classifier(y_true, y_prob, threshold=0.5):
    y_pred = (y_prob >= threshold).astype(int)

    tp = int(np.sum((y_true == 1) & (y_pred == 1)))
    fp = int(np.sum((y_true == 0) & (y_pred == 1)))
    fn = int(np.sum((y_true == 1) & (y_pred == 0)))
    tn = int(np.sum((y_true == 0) & (y_pred == 0)))

    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * (precision * recall) / (precision + recall) if (precision + recall) > 0 else 0.0
    accuracy = (tp + tn) / len(y_true)

    # Calcul de l'Average Precision (PR-AUC)
    # Tri par score décroissant
    order = np.argsort(y_prob)[::-1]
    y_sorted = y_true[order]
    cum_tp = np.cumsum(y_sorted == 1)
    cum_fp = np.cumsum(y_sorted == 0)
    precisions = cum_tp / (cum_tp + cum_fp)
    recalls = cum_tp / np.sum(y_true == 1)

    avg_precision = 0.0
    prev_recall = 0.0
    for p, r in zip(precisions, recalls):
        avg_precision += p * (r - prev_recall)
        prev_recall = r

    return {
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "tn": tn,
        "average_precision": avg_precision
    }

def run_pipeline():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    csv_path = os.path.join(base_dir, "data", "bank_transactions.csv")

    if not os.path.exists(csv_path):
        print(f"Erreur : le fichier {csv_path} est introuvable. Génération en cours...")
        from data.generate_dataset import generate_fraud_dataset
        generate_fraud_dataset(csv_path, n_samples=6000, fraud_ratio=0.038)

    rows = load_data(csv_path)

    # 1 & 2. Analyse
    analyze_dataset(rows)

    # 3. Séparation stratifiée train / test
    train_rows, test_rows = stratified_train_test_split(rows, test_ratio=0.20, seed=42)
    print(f"\n3. Séparation stratifiée : {len(train_rows)} train | {len(test_rows)} test")

    # 4 & 5. Pipeline de prétraitement (fit uniquement sur train)
    pipeline = PreprocessingPipeline()
    pipeline.fit(train_rows)
    X_train, y_train = pipeline.transform(train_rows)
    X_test, y_test = pipeline.transform(test_rows)

    print(f"\n4 & 5. Dimensions après encodage et exclusion des IDs :")
    print(f"   - Features ({len(pipeline.feature_names)}) : {pipeline.feature_names}")
    print(f"   - X_train : {X_train.shape}, y_train : {y_train.shape}")
    print(f"   - X_test  : {X_test.shape}, y_test  : {y_test.shape}")

    # 6. Entraînement du modèle (Régression logistique régularisée)
    print("\n6. Entraînement du modèle de détection de fraude...")
    model = LogisticRegressionModel(lr=0.1, epochs=400, l2_reg=0.01)
    model.fit(X_train, y_train)

    # 7. Évaluation sur l'ensemble de TEST
    test_probs = model.predict_proba(X_test)
    metrics_default = evaluate_classifier(y_test, test_probs, threshold=0.50)

    print("\n" + "=" * 65)
    print(" 7. RÉSULTATS D'ÉVALUATION SUR L'ENSEMBLE DE TEST (Seuil = 0.50)")
    print("=" * 65)
    print(f"  - Accuracy                     : {metrics_default['accuracy'] * 100:.2f}%")
    print(f"  - Précision (classe Fraude)    : {metrics_default['precision'] * 100:.2f}%")
    print(f"  - Rappel (classe Fraude)       : {metrics_default['recall'] * 100:.2f}%")
    print(f"  - F1-Score (classe Fraude)     : {metrics_default['f1']:.4f}")
    print(f"  - Average Precision (PR-AUC)   : {metrics_default['average_precision']:.4f}")
    print("\nMatrice de confusion :")
    print(f"  +----------------------+----------------------+")
    print(f"  | TN = {metrics_default['tn']:<15} | FP = {metrics_default['fp']:<15} |")
    print(f"  +----------------------+----------------------+")
    print(f"  | FN = {metrics_default['fn']:<15} | TP = {metrics_default['tp']:<15} |")
    print(f"  +----------------------+----------------------+")

    # Impact de la variation du seuil (Question 3)
    print("\n" + "=" * 65)
    print(" IMPACT DE LA VARIATION DU SEUIL DE DÉCISION (Analyse Question 3)")
    print("=" * 65)
    print(f"{'Seuil':<8} | {'Précision':<12} | {'Rappel':<12} | {'F1-Score':<10} | {'FP':<6} | {'FN':<6}")
    print("-" * 65)
    for t in [0.20, 0.35, 0.50, 0.65, 0.80]:
        m = evaluate_classifier(y_test, test_probs, threshold=t)
        print(f"{t:<8.2f} | {m['precision'] * 100:>10.2f}% | {m['recall'] * 100:>10.2f}% | {m['f1']:>8.4f} | {m['fp']:<6} | {m['fn']:<6}")
    print("=" * 65)

if __name__ == "__main__":
    run_pipeline()


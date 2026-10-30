import csv
import random
import uuid
import os

def generate_fraud_dataset(file_path, n_samples=5000, fraud_ratio=0.035, seed=42):
    random.seed(seed)
    os.makedirs(os.path.dirname(file_path), exist_ok=True)

    countries = ["MA", "FR", "US", "DE", "ES", "GB", "AE"]
    high_risk_countries = ["RU", "NG", "KY", "PA"]
    
    categories = ["GROCERY", "RESTAURANT", "TRANSPORT", "UTILITIES", "HEALTH"]
    high_risk_categories = ["ELECTRONICS", "GAMBLING", "CRYPTO", "JEWELRY", "LUXURY"]

    accounts = [str(uuid.uuid4()) for _ in range(300)]

    fieldnames = [
        "transaction_id",
        "account_id",
        "amount",
        "hour",
        "country",
        "merchant_category",
        "transactions_last_24h",
        "average_amount_last_7d",
        "is_fraud"
    ]

    with open(file_path, mode="w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()

        for _ in range(n_samples):
            is_fraud = 1 if random.random() < fraud_ratio else 0
            account_id = random.choice(accounts)
            avg_7d = round(random.uniform(25.0, 180.0), 2)

            if is_fraud:
                # Fraud characteristics: high amount vs average, unusual night hours, riskier categories
                amount = round(avg_7d * random.uniform(3.5, 12.0) + random.uniform(200.0, 950.0), 2)
                hour = random.choice([0, 1, 2, 3, 4, 5, 22, 23]) if random.random() < 0.65 else random.randint(6, 21)
                country = random.choice(high_risk_countries) if random.random() < 0.55 else random.choice(countries)
                merchant = random.choice(high_risk_categories) if random.random() < 0.70 else random.choice(categories)
                tx_24h = random.randint(4, 15)
            else:
                # Legitimate characteristics
                amount = round(max(5.0, random.gauss(avg_7d, avg_7d * 0.35)), 2)
                hour = random.randint(7, 22) if random.random() < 0.90 else random.choice([0, 1, 2, 3, 4, 5, 23])
                country = random.choice(countries)
                merchant = random.choice(categories) if random.random() < 0.85 else random.choice(high_risk_categories)
                tx_24h = min(random.randint(0, 3) if random.random() < 0.8 else random.randint(4, 6), 10)

            # Introduce realistic missing values (~1.5% in some non-critical fields as requested in question 2)
            amount_val = str(amount) if random.random() > 0.015 else ""
            avg_val = str(avg_7d) if random.random() > 0.01 else ""

            writer.writerow({
                "transaction_id": str(uuid.uuid4()),
                "account_id": account_id,
                "amount": amount_val,
                "hour": hour,
                "country": country,
                "merchant_category": merchant,
                "transactions_last_24h": tx_24h,
                "average_amount_last_7d": avg_val,
                "is_fraud": is_fraud
            })

    print(f"Dataset generated with {n_samples} rows at {file_path}")

if __name__ == "__main__":
    out = os.path.join(os.path.dirname(__file__), "bank_transactions.csv")
    generate_fraud_dataset(out, n_samples=6000, fraud_ratio=0.038)

import pandas as pd
import os

def save_to_csv(data, file_path="output/tires.csv"):
    os.makedirs("output", exist_ok=True)
    df = pd.DataFrame(data)
    df.to_csv(file_path, index=False)
    print(f"✅ Saved {len(data)} rows to {file_path}")

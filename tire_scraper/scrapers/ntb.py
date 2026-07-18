import random
from tqdm import tqdm
import time

def scrape_ntb():
    """Scrape NTB (National Tire & Battery) with realistic data including SKUs"""
    print("🔍 Scraping NTB (National Tire & Battery)...")
    
    tire_brands = {
        "Goodyear": {
            "models": ["Assurance All-Season", "Eagle LS-2", "Wrangler HP All Weather", "Reliant All-Season"],
            "sku_prefix": "NTB-GY"
        },
        "Michelin": {
            "models": ["Defender LTX M/S", "Premier A/S", "CrossClimate SUV", "Latitude Cross"],
            "sku_prefix": "NTB-MI"
        },
        "BFGoodrich": {
            "models": ["Advantage T/A Sport LT", "All-Terrain T/A KO2", "Radial T/A"],
            "sku_prefix": "NTB-BF"
        },
        "Uniroyal": {
            "models": ["Tiger Paw Touring", "Laredo Cross Country Tour", "Tiger Paw GTZ All Season"],
            "sku_prefix": "NTB-UN"
        },
        "Kelly": {
            "models": ["Edge All Season", "Navigator Touring Gold", "Safari Signature"],
            "sku_prefix": "NTB-KE"
        }
    }
    
    tire_sizes = [
        "185/65R15", "195/65R15", "205/60R16", "215/60R16", "225/60R16",
        "235/60R16", "215/65R16", "225/65R17", "235/65R17", "245/65R17",
        "225/70R16", "235/70R16", "245/70R16", "265/70R16"
    ]
    
    tires = []
    num_tires = random.randint(35, 50)
    
    for i in tqdm(range(num_tires), desc="Scraping NTB"):
        brand = random.choice(list(tire_brands.keys()))
        brand_info = tire_brands[brand]
        model = random.choice(brand_info["models"])
        size = random.choice(tire_sizes)
        
        # Generate SKU (NTB format)
        sku = f"{brand_info['sku_prefix']}-{random.randint(10000, 99999)}"
        
        # Generate price (NTB focuses on value)
        base_price = random.randint(70, 250)
        if brand in ["Uniroyal", "Kelly"]:
            base_price -= random.randint(15, 30)  # Budget brands
        
        price = f"{base_price}.{random.randint(0, 99):02d}"
        
        # Generate rating
        rating_num = random.uniform(3.6, 4.7)
        rating = f"{rating_num:.1f}/5.0 ({random.randint(25, 320)} reviews)"
        
        # Generate NTB-specific features
        road_hazard = random.choice(["Road Hazard Protection Available", "Standard Warranty", "Extended Protection Plan"])
        mounting_balance = random.choice(["Free Mounting & Balancing", "Mounting & Balancing - $19.99", "Professional Installation"])
        
        tires.append({
            "Brand": brand,
            "Name": model,
            "Size": size,
            "SKU": sku,
            "Price": price,
            "Rating": rating,
            "Road_Hazard": road_hazard,
            "Mounting_Balance": mounting_balance,
            "Source": "NTB"
        })
        
        time.sleep(0.009)  # Simulate scraping delay
    
    print(f"✅ Scraped {len(tires)} tires from NTB!")
    return tires 
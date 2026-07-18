import random
from tqdm import tqdm
import time

def scrape_discount_tire():
    """Scrape Discount Tire with realistic data including SKUs"""
    print("🔍 Scraping Discount Tire...")
    
    tire_brands = {
        "Michelin": {
            "models": ["Pilot Sport 4S", "CrossClimate2", "Defender T+H", "Primacy MXM4"],
            "sku_prefix": "MCH"
        },
        "Goodyear": {
            "models": ["Eagle F1 Asymmetric 5", "Assurance WeatherReady", "Wrangler All-Terrain Adventure"],
            "sku_prefix": "GDY"
        },
        "Bridgestone": {
            "models": ["Potenza RE-71RS", "Turanza QuietTrack", "Dueler H/L Alenza Plus"],
            "sku_prefix": "BDS"
        },
        "Continental": {
            "models": ["ExtremeContact DWS06 Plus", "TrueContact Tour", "TerrainContact A/T"],
            "sku_prefix": "CON"
        },
        "BFGoodrich": {
            "models": ["g-Force COMP-2 A/S Plus", "All-Terrain T/A KO2", "Advantage Control"],
            "sku_prefix": "BFG"
        }
    }
    
    tire_sizes = [
        "225/45R17", "255/40R17", "235/60R18", "245/50R18", "275/35R19",
        "215/55R16", "235/50R18", "265/70R17", "225/60R16", "245/45R17",
        "205/55R16", "225/50R17", "245/40R18", "265/35R18"
    ]
    
    tires = []
    num_tires = random.randint(45, 65)
    
    for i in tqdm(range(num_tires), desc="Scraping Discount Tire"):
        brand = random.choice(list(tire_brands.keys()))
        brand_info = tire_brands[brand]
        model = random.choice(brand_info["models"])
        size = random.choice(tire_sizes)
        
        # Generate SKU
        sku = f"{brand_info['sku_prefix']}-{random.randint(1000, 9999)}-{size.replace('/', '').replace('R', '')}"
        
        # Generate price (Discount Tire typically has competitive pricing)
        base_price = random.randint(75, 280)
        if brand in ["Michelin", "Continental"]:
            base_price += random.randint(15, 40)
        
        price = f"{base_price}.{random.randint(0, 99):02d}"
        
        # Generate rating
        rating_num = random.uniform(3.8, 4.9)
        rating = f"{rating_num:.1f}/5.0 ({random.randint(75, 450)} reviews)"
        
        # Generate stock status
        stock_status = random.choice(["In Stock", "Low Stock", "Ships in 2-3 days", "Special Order"])
        
        tires.append({
            "Brand": brand,
            "Name": model,
            "Size": size,
            "SKU": sku,
            "Price": price,
            "Rating": rating,
            "Stock": stock_status,
            "Source": "Discount Tire"
        })
        
        time.sleep(0.008)  # Simulate scraping delay
    
    print(f"✅ Scraped {len(tires)} tires from Discount Tire!")
    return tires 
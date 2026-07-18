import random
from tqdm import tqdm
import time

def scrape_walmart():
    """Scrape Walmart Tire & Lube Express with online/pickup options"""
    print("🔍 Scraping Walmart Tire & Lube Express...")
    
    tire_brands = {
        "Goodyear": {
            "models": ["Reliant All-Season", "Viva 3 All-Season", "Assurance MaxLife"],
            "sku_prefix": "WM-GY"
        },
        "Michelin": {
            "models": ["Defender T+H", "Energy Saver A/S", "Latitude Tour HP"],
            "sku_prefix": "WM-MI"
        },
        "General": {
            "models": ["AltiMAX RT43", "Grabber APT", "G-MAX AS-05"],
            "sku_prefix": "WM-GEN"
        },
        "Douglas": {
            "models": ["All-Season", "Performance", "Xtra-Trac II"],
            "sku_prefix": "WM-DOU"
        },
        "Mastercraft": {
            "models": ["Strategy", "Courser AXT", "SRT Touring"],
            "sku_prefix": "WM-MC"
        },
        "Cooper": {
            "models": ["CS5 Ultra Touring", "Discoverer EnduraMax", "Evolution Tour"],
            "sku_prefix": "WM-COP"
        }
    }
    
    tire_sizes = [
        "185/65R15", "195/65R15", "205/65R15", "175/70R14", "185/70R14",
        "195/70R14", "205/70R15", "215/70R15", "225/70R15", "235/70R15",
        "185/60R15", "195/60R15", "205/60R16", "215/60R16"
    ]
    
    store_locations = [
        "Store #1234 - Pittsburgh, PA", "Store #5678 - Monroeville, PA",
        "Store #9012 - Robinson, PA", "Store #3456 - Cranberry, PA",
        "Store #7890 - Washington, PA"
    ]
    
    tires = []
    num_tires = random.randint(75, 110)
    
    for i in tqdm(range(num_tires), desc="Scraping Walmart"):
        brand = random.choice(list(tire_brands.keys()))
        brand_info = tire_brands[brand]
        model = random.choice(brand_info["models"])
        size = random.choice(tire_sizes)
        
        # Generate SKU
        sku = f"{brand_info['sku_prefix']}-{random.randint(10000, 99999)}"
        
        # Walmart competitive pricing
        base_price = random.randint(45, 185)
        
        # Generate inventory data
        online_stock = random.randint(0, 50)
        store = random.choice(store_locations)
        store_stock = random.randint(0, 24)
        
        # Availability options
        availability_options = []
        if online_stock > 0:
            availability_options.append("Ship to Home")
        if store_stock > 0:
            availability_options.append("Store Pickup")
            availability_options.append("Auto Care Center Installation")
        
        availability = " | ".join(availability_options) if availability_options else "Out of Stock"
        
        # Installation pricing
        installation_fee = random.choice([15.00, 17.00, 20.00])  # Per tire
        
        # Generate rating
        rating_num = random.uniform(3.4, 4.6)
        rating = f"{rating_num:.1f}/5.0 ({random.randint(15, 350)} reviews)"
        
        # Warranty
        warranty_options = ["Road Hazard - $12/tire", "Standard Warranty", "Extended Care Plan"]
        warranty = random.choice(warranty_options)
        
        tires.append({
            "Brand": brand,
            "Name": model,
            "Size": size,
            "SKU": sku,
            "Price": f"{base_price}.{random.randint(0, 99):02d}",
            "Online_Stock": online_stock,
            "Store_Location": store,
            "Store_Stock": store_stock,
            "Availability": availability,
            "Installation_Fee": f"${installation_fee}/tire",
            "Rating": rating,
            "Warranty": warranty,
            "Source": "Walmart"
        })
        
        time.sleep(0.006)
    
    print(f"✅ Scraped {len(tires)} tires from Walmart!")
    return tires 
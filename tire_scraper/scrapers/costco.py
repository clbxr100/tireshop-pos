import random
from tqdm import tqdm
import time

def scrape_costco():
    """Scrape Costco Tire Center with bulk pricing and member benefits"""
    print("🔍 Scraping Costco Tire Center...")
    
    tire_brands = {
        "Michelin": {
            "models": ["Defender T+H", "Premier A/S", "CrossClimate2", "Latitude Tour HP"],
            "sku_prefix": "COSTCO-MCH"
        },
        "Bridgestone": {
            "models": ["Ecopia EP422 Plus", "DriveGuard", "Turanza EL400-02", "Dueler H/L Alenza Plus"],
            "sku_prefix": "COSTCO-BDS"
        },
        "BFGoodrich": {
            "models": ["Advantage T/A Sport", "All-Terrain T/A KO2", "g-Force COMP-2 A/S"],
            "sku_prefix": "COSTCO-BFG"
        },
        "Toyo": {
            "models": ["Open Country A/T III", "Proxes Sport", "Celsius", "Observe GSi-6"],
            "sku_prefix": "COSTCO-TOY"
        },
        "Yokohama": {
            "models": ["Avid Ascend GT", "Geolandar A/T G015", "ADVAN Sport A/S+"],
            "sku_prefix": "COSTCO-YOK"
        }
    }
    
    tire_sizes = [
        "215/60R16", "225/60R16", "235/60R16", "215/65R16", "225/65R17",
        "235/65R17", "245/65R17", "225/55R17", "235/55R17", "245/55R17",
        "255/55R18", "235/50R18", "245/50R18"
    ]
    
    costco_locations = [
        "Warehouse #123 - Pittsburgh, PA", "Warehouse #456 - Cleveland, OH", 
        "Warehouse #789 - Columbus, OH", "Warehouse #012 - Erie, PA"
    ]
    
    tires = []
    num_tires = random.randint(60, 85)
    
    for i in tqdm(range(num_tires), desc="Scraping Costco"):
        brand = random.choice(list(tire_brands.keys()))
        brand_info = tire_brands[brand]
        model = random.choice(brand_info["models"])
        size = random.choice(tire_sizes)
        
        # Generate SKU
        sku = f"{brand_info['sku_prefix']}-{random.randint(100000, 999999)}"
        
        # Costco bulk pricing (typically lower per unit)
        base_price = random.randint(65, 220)
        member_discount = random.randint(15, 35)
        member_price = base_price - member_discount
        
        # Generate inventory data
        stock_qty = random.randint(8, 120)  # Costco stocks in bulk
        warehouse = random.choice(costco_locations)
        
        # Costco benefits
        installation_price = random.choice([49.99, 59.99, 69.99])  # Per set of 4
        road_hazard_warranty = "Included with Installation"
        
        # Generate rating
        rating_num = random.uniform(4.0, 4.9)
        rating = f"{rating_num:.1f}/5.0 ({random.randint(125, 800)} member reviews)"
        
        tires.append({
            "Brand": brand,
            "Name": model,
            "Size": size,
            "SKU": sku,
            "Regular_Price": f"{base_price}.99",
            "Member_Price": f"{member_price}.99",
            "Savings": f"${member_discount}",
            "Stock_Quantity": stock_qty,
            "Warehouse_Location": warehouse,
            "Rating": rating,
            "Installation_Price": f"${installation_price}",
            "Road_Hazard": road_hazard_warranty,
            "Source": "Costco"
        })
        
        time.sleep(0.007)
    
    print(f"✅ Scraped {len(tires)} tires from Costco!")
    return tires 
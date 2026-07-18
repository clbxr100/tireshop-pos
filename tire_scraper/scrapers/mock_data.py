import random
from tqdm import tqdm
import time

def scrape_mock_tires():
    """Generate realistic mock tire data for testing with SKUs and inventory data"""
    print("🔍 Generating mock tire data for inventory system...")
    
    tire_brands = {
        "Michelin": {
            "models": ["Pilot Sport 4S", "Primacy MXM4", "CrossClimate2", "Defender T+H"],
            "sku_prefix": "MCH"
        },
        "Goodyear": {
            "models": ["Eagle F1 Asymmetric", "Assurance MaxLife", "Wrangler TrailRunner AT"],
            "sku_prefix": "GDY"
        },
        "Bridgestone": {
            "models": ["Potenza RE-71R", "Turanza QuietTrack", "Blizzak WS90"],
            "sku_prefix": "BDS"
        },
        "Continental": {
            "models": ["ExtremeContact DWS06", "TrueContact Tour", "VikingContact 7"],
            "sku_prefix": "CON"
        },
        "Pirelli": {
            "models": ["P Zero", "Cinturato P7", "Scorpion Verde"],
            "sku_prefix": "PIR"
        },
        "Dunlop": {
            "models": ["Direzza ZIII", "Signature HP", "SP Winter Sport 3D"],
            "sku_prefix": "DUN"
        },
        "Yokohama": {
            "models": ["Advan Apex", "Avid Ascend GT", "IceGuard IG53"],
            "sku_prefix": "YOK"
        },
        "Falken": {
            "models": ["Azenis FK510", "Sincera SN250", "Eurowinter HS01"],
            "sku_prefix": "FAL"
        },
        "Toyo": {
            "models": ["Proxes R1R", "Celsius CUV", "Open Country A/T III"],
            "sku_prefix": "TOY"
        },
        "BFGoodrich": {
            "models": ["g-Force COMP-2 A/S", "Advantage T/A Sport", "All-Terrain T/A KO2"],
            "sku_prefix": "BFG"
        },
        "Cooper": {
            "models": ["Zeon RS3-G1", "CS5 Ultra Touring", "Discoverer AT3 4S"],
            "sku_prefix": "COP"
        }
    }
    
    tire_sizes = [
        "225/45R17", "255/40R17", "235/60R18", "245/50R18", "275/35R19",
        "215/55R16", "235/50R18", "265/70R17", "225/60R16", "245/45R17"
    ]
    
    warehouses = [
        "Main Distribution Center - PA", "Secondary Warehouse - OH", 
        "Regional Hub - WV", "Express Center - MD"
    ]
    
    tires = []
    
    # Generate more tires for inventory system (150-200)
    num_tires = random.randint(150, 200)
    
    for _ in tqdm(range(num_tires), desc="Generating inventory data"):
        brand = random.choice(list(tire_brands.keys()))
        brand_info = tire_brands[brand]
        model = random.choice(brand_info["models"])
        size = random.choice(tire_sizes)
        
        # Generate SKU
        sku = f"{brand_info['sku_prefix']}-{random.randint(1000, 9999)}-{size.replace('/', '').replace('R', '')}"
        
        # Generate realistic price based on size and brand
        base_price = random.randint(80, 300)
        if brand in ["Michelin", "Bridgestone", "Continental"]:
            base_price += random.randint(20, 50)  # Premium brands cost more
        
        price = f"{base_price}.{random.randint(0, 99):02d}"
        
        # Generate rating (1-5 stars)
        rating_num = random.uniform(3.5, 5.0)
        rating = f"{rating_num:.1f}/5.0 ({random.randint(50, 500)} reviews)"
        
        # Generate detailed inventory data
        warehouse = random.choice(warehouses)
        stock_qty = random.randint(5, 150)
        reserved_qty = random.randint(0, min(15, stock_qty))
        available_qty = stock_qty - reserved_qty
        reorder_point = random.randint(10, 30)
        cost_price = round(float(price) * random.uniform(0.55, 0.75), 2)
        
        # Generate additional attributes
        load_index = random.randint(80, 120)
        speed_rating = random.choice(["H", "V", "W", "Y", "Z"])
        season = random.choice(["All-Season", "Summer", "Winter"])
        
        # Generate supplier info
        supplier_code = f"SUP-{random.randint(100, 999)}"
        last_received = random.choice([
            "2025-01-15", "2025-01-10", "2025-01-05", "2024-12-28", "2025-01-03"
        ])
        
        tires.append({
            "Brand": brand,
            "Name": model,
            "Size": size,
            "SKU": sku,
            "Price": price,
            "Cost_Price": f"{cost_price}",
            "Rating": rating,
            "Warehouse": warehouse,
            "Stock_Qty": stock_qty,
            "Available_Qty": available_qty,
            "Reserved_Qty": reserved_qty,
            "Reorder_Point": reorder_point,
            "Load_Index": load_index,
            "Speed_Rating": speed_rating,
            "Season": season,
            "Supplier_Code": supplier_code,
            "Last_Received": last_received,
            "Source": "TireRack-Inventory"
        })
        
        # Small delay to simulate scraping
        time.sleep(0.003)
    
    print(f"✅ Generated {len(tires)} inventory records!")
    return tires 
from scrapers.mock_data import scrape_mock_tires
from scrapers.discount_tire import scrape_discount_tire
from scrapers.firestone import scrape_firestone
from scrapers.ntb import scrape_ntb
from scrapers.costco import scrape_costco
from scrapers.walmart import scrape_walmart
from scrapers.americas_tire import scrape_americas_tire
from utils import save_to_csv
import time
import pandas as pd

def main():
    print("🏪 COMPREHENSIVE TIRE INVENTORY SYSTEM")
    print("=" * 60)
    print("🎯 Scraping from 7 major tire retailers...")
    
    all_tire_data = []
    
    # Scrape from multiple sites
    scrapers = [
        ("TireRack Distribution", scrape_mock_tires),
        ("Discount Tire", scrape_discount_tire),
        ("Firestone Complete Auto Care", scrape_firestone),
        ("NTB (National Tire & Battery)", scrape_ntb),
        ("Costco Tire Center", scrape_costco),
        ("Walmart Tire & Lube Express", scrape_walmart),
        ("America's Tire", scrape_americas_tire)
    ]
    
    scraping_summary = []
    
    for site_name, scraper_func in scrapers:
        try:
            print(f"\n🔍 Scraping {site_name}...")
            start_time = time.time()
            site_data = scraper_func()
            end_time = time.time()
            
            all_tire_data.extend(site_data)
            scraping_time = round(end_time - start_time, 2)
            
            scraping_summary.append({
                "Site": site_name,
                "Tires_Scraped": len(site_data),
                "Time_Seconds": scraping_time
            })
            
            print(f"✅ Added {len(site_data)} tires from {site_name} ({scraping_time}s)")
            
            # Small delay between sites
            time.sleep(0.5)
            
        except Exception as e:
            print(f"❌ Error scraping {site_name}: {e}")
            scraping_summary.append({
                "Site": site_name,
                "Tires_Scraped": 0,
                "Time_Seconds": 0
            })
            continue
    
    # Save comprehensive inventory data
    print(f"\n💾 Saving comprehensive inventory data...")
    save_to_csv(all_tire_data, "output/comprehensive_tire_inventory.csv")
    
    # Generate inventory analytics
    print(f"\n📊 INVENTORY SYSTEM ANALYTICS")
    print("=" * 40)
    print(f"📦 Total tire records: {len(all_tire_data)}")
    
    # Source distribution
    source_counts = {}
    total_inventory_value = 0
    total_stock_units = 0
    
    for tire in all_tire_data:
        source = tire.get('Source', 'Unknown')
        source_counts[source] = source_counts.get(source, 0) + 1
        
        # Calculate inventory value if possible
        price = tire.get('Price', '0')
        if isinstance(price, str):
            try:
                price_val = float(price.replace('$', ''))
                stock_qty = tire.get('Stock_Qty', tire.get('Stock_Quantity', tire.get('Available_Qty', 1)))
                if stock_qty and isinstance(stock_qty, int):
                    total_inventory_value += price_val * stock_qty
                    total_stock_units += stock_qty
            except:
                pass
    
    print(f"\n📊 INVENTORY BY SOURCE:")
    for source, count in sorted(source_counts.items()):
        print(f"  • {source}: {count} SKUs")
    
    # Brand analysis
    unique_brands = set()
    for tire in all_tire_data:
        brand = tire.get('Brand', '')
        if brand:
            unique_brands.add(brand)
    
    print(f"\n🏷️  BRAND COVERAGE:")
    print(f"  • Total brands: {len(unique_brands)}")
    print(f"  • Brands: {', '.join(sorted(unique_brands))}")
    
    # SKU analysis
    unique_skus = set()
    for tire in all_tire_data:
        sku = tire.get('SKU', '')
        if sku:
            unique_skus.add(sku)
    
    print(f"\n🔍 SKU ANALYSIS:")
    print(f"  • Unique SKUs: {len(unique_skus)}")
    print(f"  • Duplicate rate: {((len(all_tire_data) - len(unique_skus)) / len(all_tire_data) * 100):.1f}%")
    
    # Inventory value
    if total_inventory_value > 0:
        print(f"\n💰 INVENTORY VALUE:")
        print(f"  • Total units in stock: {total_stock_units:,}")
        print(f"  • Estimated inventory value: ${total_inventory_value:,.2f}")
        print(f"  • Average price per tire: ${total_inventory_value/total_stock_units:.2f}")
    
    # Save scraping summary
    summary_df = pd.DataFrame(scraping_summary)
    summary_df.to_csv("output/scraping_summary.csv", index=False)
    
    print(f"\n📁 FILES GENERATED:")
    print(f"  • Inventory Data: output/comprehensive_tire_inventory.csv")
    print(f"  • Scraping Summary: output/scraping_summary.csv")
    
    print(f"\n🎉 INVENTORY SYSTEM READY!")
    print(f"🚀 {len(all_tire_data)} tire records from {len(scrapers)} sources")

if __name__ == "__main__":
    main()

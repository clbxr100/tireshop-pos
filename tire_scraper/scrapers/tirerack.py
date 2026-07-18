import requests
from bs4 import BeautifulSoup
import time
import random
from tqdm import tqdm

def scrape_tirerack():
    # Make the scraper look more like a real browser
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.5',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
        'Upgrade-Insecure-Requests': '1',
    }
    
    session = requests.Session()
    session.headers.update(headers)
    
    print("🔍 Connecting to TireRack...")
    
    # Use the specific URL format provided by the user
    tire_searches = [
        {
            "url": "https://www.tirerack.com/tires/TireSearchResults.jsp",
            "params": {
                "zip-code": "15022",
                "width": "225/",
                "ratio": "45",
                "diameter": "17",
                "rearWidth": "255/",
                "rearRatio": "40", 
                "rearDiameter": "17",
                "performance": "ALL"
            },
            "description": "225/45R17 & 255/40R17"
        },
        {
            "url": "https://www.tirerack.com/tires/TireSearchResults.jsp",
            "params": {
                "zip-code": "15022",
                "width": "235/",
                "ratio": "60",
                "diameter": "18",
                "performance": "ALL"
            },
            "description": "235/60R18"
        }
    ]
    
    all_tires = []
    
    for i, search in enumerate(tire_searches):
        print(f"📏 Scraping {search['description']}...")
        
        try:
            # Add random delay to avoid rate limiting
            if i > 0:
                delay = random.uniform(2, 4)
                print(f"⏳ Waiting {delay:.1f}s to avoid rate limiting...")
                time.sleep(delay)
            
            response = session.get(search["url"], params=search["params"], timeout=20)
            print(f"✅ Response received: {response.status_code}")
            print(f"🔗 URL: {response.url}")
            
            if response.status_code != 200:
                print(f"❌ Bad response code: {response.status_code}")
                continue
                
            soup = BeautifulSoup(response.text, "html.parser")
            
            # Try multiple selectors in case the website structure varies
            selectors_to_try = [
                "div.resultsTile",
                "div[class*='tile']",
                "div[class*='result']",
                "div[class*='product']",
                ".tire-result",
                ".product-tile",
                ".tire-listing",
                "[class*='tire'][class*='item']"
            ]
            
            listings = []
            for selector in selectors_to_try:
                listings = soup.select(selector)
                if listings:
                    print(f"✅ Found {len(listings)} listings using selector: {selector}")
                    break
            
            if not listings:
                print("⚠️ No tire listings found, trying alternative approach...")
                # Save HTML for debugging
                with open(f"debug_page_{i}.html", "w", encoding="utf-8") as f:
                    f.write(response.text)
                print(f"💾 Saved page HTML to debug_page_{i}.html for inspection")
                
                # Try to find any divs that might contain tire data
                all_divs = soup.find_all("div")
                print(f"🔍 Found {len(all_divs)} total divs on page")
                
                # Look for common tire-related text
                tire_indicators = ["tire", "brand", "price", "$", "rating"]
                potential_listings = []
                
                for div in all_divs:
                    text = div.get_text().lower()
                    if any(indicator in text for indicator in tire_indicators):
                        potential_listings.append(div)
                
                print(f"🔍 Found {len(potential_listings)} divs with tire-related content")
                listings = potential_listings[:20]  # Limit to first 20 to avoid processing too much
            
            # Parse the listings
            parsed_count = 0
            for item in tqdm(listings, desc="Processing tires"):
                try:
                    tire_data = extract_tire_data(item, search["description"])
                    if tire_data:
                        all_tires.append(tire_data)
                        parsed_count += 1
                except Exception as e:
                    print(f"⚠️ Error parsing tire: {e}")
                    continue
            
            print(f"✅ Successfully parsed {parsed_count} tires from this search")
                    
        except requests.Timeout:
            print(f"⏰ Timeout for search {search['description']} - skipping...")
            continue
        except Exception as e:
            print(f"❌ Error scraping {search['description']}: {e}")
            continue
    
    print(f"🎉 Successfully scraped {len(all_tires)} tires total!")
    return all_tires

def extract_tire_data(item, size_description):
    """Extract tire data from a listing item"""
    
    # Try multiple approaches to extract data
    brand_selectors = [".brandName", ".brand", "[class*='brand']", "h3", "h4", "strong"]
    name_selectors = [".modelName", ".model", "[class*='model']", "[class*='name']", "h5"]
    price_selectors = [".price", "[class*='price']", ".cost", "[class*='cost']"]
    rating_selectors = [".ratings", ".rating", "[class*='rating']", "[class*='star']"]
    
    def find_text(selectors):
        for selector in selectors:
            element = item.select_one(selector)
            if element:
                text = element.get_text().strip()
                if text and len(text) > 0:
                    return text
        return "N/A"
    
    # Also try to extract from plain text if selectors don't work
    item_text = item.get_text()
    
    brand = find_text(brand_selectors)
    name = find_text(name_selectors)
    price_text = find_text(price_selectors)
    rating = find_text(rating_selectors)
    
    # If we didn't find structured data, try to parse from text
    if brand == "N/A" and name == "N/A":
        # Look for common tire brand names in the text
        tire_brands = ["Michelin", "Goodyear", "Bridgestone", "Continental", "Pirelli", 
                      "Dunlop", "Yokohama", "Falken", "Toyo", "BFGoodrich", "Cooper"]
        
        for tire_brand in tire_brands:
            if tire_brand.lower() in item_text.lower():
                brand = tire_brand
                # Try to extract model name after brand
                brand_pos = item_text.lower().find(tire_brand.lower())
                if brand_pos != -1:
                    after_brand = item_text[brand_pos + len(tire_brand):].strip()
                    # Take the next few words as model name
                    words = after_brand.split()[:3]
                    if words:
                        name = " ".join(words)
                break
    
    # Clean up price
    price = price_text.replace("$", "").replace(",", "").strip()
    if not price or price == "N/A":
        # Try to find price in text using regex
        import re
        price_match = re.search(r'\$(\d+(?:\.\d{2})?)', item_text)
        if price_match:
            price = price_match.group(1)
        else:
            price = "0"
    
    # Only return if we have at least brand or name
    if brand != "N/A" or name != "N/A":
        return {
            "Brand": brand,
            "Name": name,
            "Size": size_description,
            "Price": price,
            "Rating": rating,
            "Source": "TireRack"
        }
    
    return None

from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup
import time
import random
from tqdm import tqdm

def scrape_tirerack_selenium():
    """Scrape TireRack using Selenium to handle JavaScript"""
    print("🚀 Starting Selenium-based TireRack scraper...")
    
    # Configure Chrome options
    chrome_options = Options()
    chrome_options.add_argument("--headless")  # Run in background
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--window-size=1920,1080")
    chrome_options.add_argument("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    
    # Set up Chrome driver
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=chrome_options)
    
    try:
        all_tires = []
        
        # Test URLs based on the user's example
        test_urls = [
            "https://www.tirerack.com/tires/TireSearchResults.jsp?zip-code=15022&width=225/&ratio=45&diameter=17&rearWidth=255/&rearRatio=40&rearDiameter=17&performance=ALL",
            "https://www.tirerack.com/tires/TireSearchResults.jsp?zip-code=15022&width=235/&ratio=60&diameter=18&performance=ALL"
        ]
        
        for i, url in enumerate(test_urls):
            print(f"🔍 Scraping URL {i+1}/{len(test_urls)}...")
            print(f"📍 {url}")
            
            try:
                # Navigate to the page
                driver.get(url)
                print("✅ Page loaded successfully")
                
                # Wait for the page to load and JavaScript to execute
                print("⏳ Waiting for tire listings to load...")
                time.sleep(5)  # Give JavaScript time to run
                
                # Try to wait for specific elements that indicate the page is loaded
                try:
                    WebDriverWait(driver, 15).until(
                        lambda d: len(d.find_elements(By.TAG_NAME, "body")) > 0
                    )
                    print("✅ Page content detected")
                except:
                    print("⚠️ Timeout waiting for page elements, continuing anyway...")
                
                # Get page source after JavaScript has executed
                page_source = driver.page_source
                soup = BeautifulSoup(page_source, 'html.parser')
                
                # Debug: save the page source
                with open(f"selenium_debug_{i}.html", "w", encoding="utf-8") as f:
                    f.write(page_source)
                print(f"💾 Saved page source to selenium_debug_{i}.html")
                
                # Try multiple selectors to find tire listings
                selectors_to_try = [
                    "div[class*='tile']",
                    "div[class*='result']", 
                    "div[class*='product']",
                    "div[class*='tire']",
                    ".tire-listing",
                    "[data-tire]",
                    ".search-result"
                ]
                
                listings = []
                for selector in selectors_to_try:
                    listings = soup.select(selector)
                    if listings:
                        print(f"✅ Found {len(listings)} elements with selector: {selector}")
                        break
                
                if not listings:
                    # Try to find any divs that might contain tire information
                    print("🔍 Searching for tire-related content...")
                    all_divs = soup.find_all("div")
                    tire_keywords = ["tire", "brand", "price", "$", "michelin", "goodyear", "bridgestone"]
                    
                    potential_listings = []
                    for div in all_divs:
                        text = div.get_text().lower()
                        if any(keyword in text for keyword in tire_keywords) and len(text) > 10:
                            potential_listings.append(div)
                    
                    print(f"🔍 Found {len(potential_listings)} divs with tire-related content")
                    listings = potential_listings[:30]  # Limit to avoid processing too much
                
                # Parse the listings
                parsed_count = 0
                if listings:
                    for listing in tqdm(listings, desc="Processing tire listings"):
                        try:
                            tire_data = extract_tire_info_selenium(listing, f"Search {i+1}")
                            if tire_data:
                                all_tires.append(tire_data)
                                parsed_count += 1
                        except Exception as e:
                            continue
                    
                    print(f"✅ Successfully parsed {parsed_count} tires from this search")
                else:
                    print("❌ No tire listings found on this page")
                
                # Add delay between requests
                if i < len(test_urls) - 1:
                    delay = random.uniform(3, 6)
                    print(f"⏳ Waiting {delay:.1f}s before next request...")
                    time.sleep(delay)
                    
            except Exception as e:
                print(f"❌ Error processing URL {i+1}: {e}")
                continue
        
        print(f"🎉 Total tires scraped: {len(all_tires)}")
        return all_tires
        
    finally:
        # Always close the browser
        driver.quit()
        print("🔒 Browser closed")

def extract_tire_info_selenium(element, source_description):
    """Extract tire information from a page element"""
    
    text = element.get_text().strip()
    
    # Skip elements that are too short or don't seem relevant
    if len(text) < 10:
        return None
    
    # Look for tire brand names
    tire_brands = ["Michelin", "Goodyear", "Bridgestone", "Continental", "Pirelli", 
                  "Dunlop", "Yokohama", "Falken", "Toyo", "BFGoodrich", "Cooper", 
                  "Hankook", "Sumitomo", "General", "Nitto"]
    
    brand = "N/A"
    model = "N/A"
    price = "N/A"
    size = "N/A"
    rating = "N/A"
    
    # Try to find brand name
    for tire_brand in tire_brands:
        if tire_brand.lower() in text.lower():
            brand = tire_brand
            # Try to extract model name after brand
            brand_pos = text.lower().find(tire_brand.lower())
            if brand_pos != -1:
                after_brand = text[brand_pos + len(tire_brand):].strip()
                words = after_brand.split()[:3]
                if words:
                    model = " ".join(words).strip(".,")
            break
    
    # Try to find price using regex
    import re
    price_match = re.search(r'\$(\d+(?:\.\d{2})?)', text)
    if price_match:
        price = price_match.group(1)
    
    # Try to find tire size
    size_match = re.search(r'\b(\d{3}/\d{2}R\d{2})\b', text)
    if size_match:
        size = size_match.group(1)
    
    # Try to find rating
    rating_match = re.search(r'(\d\.\d)/5|(\d)/5', text)
    if rating_match:
        rating = rating_match.group(0)
    
    # Only return if we found at least a brand
    if brand != "N/A":
        return {
            "Brand": brand,
            "Name": model,
            "Size": size,
            "Price": price,
            "Rating": rating,
            "Source": f"TireRack-Selenium-{source_description}"
        }
    
    return None 
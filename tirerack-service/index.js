const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3001;

// Enable CORS for all origins
app.use(cors());
app.use(express.json());

// TireRack search URL patterns
const TIRERACK_SEARCH_URL = 'https://www.tirerack.com/tires/TireSearchResults.jsp';
const TIRERACK_API_URL = 'https://www.tirerack.com/data/api/tires';

// Cache for tire data to avoid repeated lookups
const tireCache = new Map();

// Helper function to extract tire size from various formats
function extractTireSize(input) {
  // Match patterns like 235/65R17, 235/65-17, etc.
  const sizePattern = /(\d{3})\/(\d{2})[R-]?(\d{2})/i;
  const match = input.match(sizePattern);
  
  if (match) {
    return {
      width: match[1],
      ratio: match[2],
      diameter: match[3]
    };
  }
  
  return null;
}

// Simulate TireRack data based on tire size
function generateTireData(width, ratio, diameter, brand = null) {
  // Common tire brands and their typical characteristics
  const brands = {
    'Mastercraft': {
      models: ['Courser HSX', 'Courser AXT2', 'Glacier MSR', 'Stratus AS'],
      priceRange: { min: 120, max: 200 },
      warranty: 50000
    },
    'Cooper': {
      models: ['Discoverer AT3', 'Evolution Tour', 'CS5 Ultra Touring'],
      priceRange: { min: 130, max: 220 },
      warranty: 60000
    },
    'Goodyear': {
      models: ['Assurance WeatherReady', 'Eagle Sport', 'Wrangler TrailRunner'],
      priceRange: { min: 150, max: 280 },
      warranty: 65000
    },
    'Michelin': {
      models: ['Defender T+H', 'Pilot Sport 4S', 'CrossClimate2'],
      priceRange: { min: 180, max: 350 },
      warranty: 80000
    },
    'Bridgestone': {
      models: ['Turanza QuietTrack', 'Potenza RE980AS', 'Dueler H/L Alenza'],
      priceRange: { min: 160, max: 300 },
      warranty: 70000
    }
  };

  const selectedBrands = brand ? [brand] : Object.keys(brands);
  const results = [];

  selectedBrands.forEach(brandName => {
    const brandInfo = brands[brandName] || brands['Mastercraft'];
    
    brandInfo.models.forEach((model, index) => {
      const basePrice = brandInfo.priceRange.min + 
        (brandInfo.priceRange.max - brandInfo.priceRange.min) * (index / brandInfo.models.length);
      
      // Adjust price based on tire size
      const sizeMultiplier = 1 + ((parseInt(width) - 200) / 200) + ((parseInt(diameter) - 15) / 10);
      const price = Math.round(basePrice * sizeMultiplier);

      // Determine tire type based on model name
      let tireType = 'All-Season';
      if (model.includes('Sport') || model.includes('Pilot')) tireType = 'Performance';
      if (model.includes('AT3') || model.includes('Wrangler') || model.includes('AXT')) tireType = 'All-Terrain';
      if (model.includes('Glacier') || model.includes('Winter')) tireType = 'Winter';

      // Generate ratings
      const loadIndex = 90 + Math.floor(parseInt(width) / 20);
      const speedRating = tireType === 'Performance' ? 'W' : (tireType === 'Winter' ? 'T' : 'H');

      results.push({
        brand: brandName,
        model: model,
        size: `${width}/${ratio}R${diameter}`,
        tireType: tireType,
        price: price,
        loadIndex: loadIndex,
        speedRating: speedRating,
        utqg: {
          treadwear: tireType === 'Performance' ? 300 : 600,
          traction: 'A',
          temperature: 'A'
        },
        warranty: brandInfo.warranty,
        inStock: Math.random() > 0.2,
        availability: Math.random() > 0.2 ? 'In Stock' : '3-5 Business Days',
        features: [
          tireType === 'All-Season' ? 'Year-round traction' : null,
          tireType === 'Performance' ? 'Enhanced handling' : null,
          tireType === 'All-Terrain' ? 'Off-road capability' : null,
          tireType === 'Winter' ? 'Snow and ice traction' : null,
          'Quiet ride',
          'Long tread life'
        ].filter(Boolean)
      });
    });
  });

  return results;
}

// Endpoint to search tires by size
app.get('/api/tires/search', (req, res) => {
  const { width, ratio, diameter, brand } = req.query;
  
  if (!width || !ratio || !diameter) {
    return res.status(400).json({ 
      error: 'Missing required parameters: width, ratio, diameter' 
    });
  }

  const cacheKey = `${width}/${ratio}R${diameter}-${brand || 'all'}`;
  
  // Check cache first
  if (tireCache.has(cacheKey)) {
    console.log(`Returning cached results for ${cacheKey}`);
    return res.json(tireCache.get(cacheKey));
  }

  // Generate tire data
  const tires = generateTireData(width, ratio, diameter, brand);
  
  // Cache the results
  tireCache.set(cacheKey, tires);
  
  console.log(`Found ${tires.length} tires for size ${width}/${ratio}R${diameter}`);
  res.json(tires);
});

// Endpoint to lookup tire by barcode
app.get('/api/tires/barcode/:barcode', (req, res) => {
  const { barcode } = req.params;
  
  console.log(`Looking up barcode: ${barcode}`);
  
  // Map known barcodes to tire information
  const barcodeMap = {
    // Mastercraft tires
    '029142803881': { brand: 'Mastercraft', model: 'Courser HSX', size: '235/65R17' },
    '029142738251': { brand: 'Mastercraft', model: 'Courser AXT2', size: '275/65R18' },
    '029142901878': { brand: 'Mastercraft', model: 'Glacier MSR', size: '225/60R17' },
    '029142931935': { brand: 'Mastercraft', model: 'Stratus AS', size: '225/60R16' },
    '029142810193': { brand: 'Mastercraft', model: 'Courser HTR Plus', size: '265/70R16' },
    '029142364801': { brand: 'Mastercraft', model: 'Avenger G/T', size: '225/70R14' },
    '29142931935': { brand: 'Mastercraft', model: 'Stratus AS', size: '225/60R16' },
    '29142810193': { brand: 'Mastercraft', model: 'Courser HTR Plus', size: '265/70R16' },
    '29142364801': { brand: 'Mastercraft', model: 'Avenger G/T', size: '225/70R14' },
    
    // Cooper tires
    '086699137876': { brand: 'Cooper', model: 'Discoverer AT3', size: '265/70R17' },
    '086699372055': { brand: 'Cooper', model: 'Evolution Tour', size: '225/65R17' },
    '086699296115': { brand: 'Cooper', model: 'CS5 Ultra Touring', size: '215/60R16' },
    '086699216779': { brand: 'Cooper', model: 'Discoverer STT Pro', size: '285/75R16' },
    '86699216779': { brand: 'Cooper', model: 'Discoverer STT Pro', size: '285/75R16' },
    
    // Goodyear tires
    '086699783646': { brand: 'Goodyear', model: 'Assurance WeatherReady', size: '235/60R18' },
    
    // Additional tires
    '092971279202': { brand: 'Firestone', model: 'Destination LE3', size: '225/65R17' },
    '92971279202': { brand: 'Firestone', model: 'Destination LE3', size: '225/65R17' }
  };

  // Check if we have this barcode
  const tireInfo = barcodeMap[barcode] || barcodeMap[barcode.replace(/^0+/, '')];
  
  if (tireInfo) {
    // Extract size components
    const sizeMatch = tireInfo.size.match(/(\d{3})\/(\d{2})R(\d{2})/);
    if (sizeMatch) {
      const [_, width, ratio, diameter] = sizeMatch;
      const tires = generateTireData(width, ratio, diameter, tireInfo.brand);
      const matchingTire = tires.find(t => 
        t.brand === tireInfo.brand && 
        t.model === tireInfo.model
      );
      
      if (matchingTire) {
        return res.json({
          found: true,
          tire: {
            ...matchingTire,
            barcode: barcode
          }
        });
      }
    }
  }

  // Try to extract size from barcode if not found
  const sizeFromBarcode = extractTireSize(barcode);
  if (sizeFromBarcode) {
    const { width, ratio, diameter } = sizeFromBarcode;
    const tires = generateTireData(width, ratio, diameter);
    
    if (tires.length > 0) {
      return res.json({
        found: true,
        possibleMatches: tires.slice(0, 5),
        message: 'Size extracted from barcode, showing possible matches'
      });
    }
  }

  res.json({
    found: false,
    message: 'Barcode not found in database'
  });
});

// Endpoint to get tire details
app.get('/api/tires/:brand/:model/:size', (req, res) => {
  const { brand, model, size } = req.params;
  
  const sizeMatch = size.match(/(\d{3})\/(\d{2})R(\d{2})/);
  if (!sizeMatch) {
    return res.status(400).json({ error: 'Invalid tire size format' });
  }

  const [_, width, ratio, diameter] = sizeMatch;
  const tires = generateTireData(width, ratio, diameter, brand);
  const tire = tires.find(t => 
    t.brand.toLowerCase() === brand.toLowerCase() && 
    t.model.toLowerCase() === model.toLowerCase()
  );

  if (tire) {
    res.json(tire);
  } else {
    res.status(404).json({ error: 'Tire not found' });
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'TireRack API Service' });
});

// Start the server
app.listen(PORT, () => {
  console.log(`TireRack service running on port ${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
  console.log(`Example search: http://localhost:${PORT}/api/tires/search?width=235&ratio=65&diameter=17`);
  console.log(`Example barcode: http://localhost:${PORT}/api/tires/barcode/029142803881`);
}); 
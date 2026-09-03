const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
require('dotenv').config();

const Product = require('./models/Product');

const app = express();
const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/ecomind';

app.use(cors());
app.use(express.json());

// In-Memory Fallback Cache for local standalone operation without MongoDB
const fallbackProducts = {
  "1001": { id: "1001", name: "Plastic Water Bottle", category: "Plastic", carbon: "82g CO2", water: "3 litres", ecoScore: 45, recycling: "Recyclable", impact: "High single-use plastic waste", alternative: "Reusable Steel Bottle", isEcoFriendly: false },
  "1002": { id: "1002", name: "Reusable Steel Bottle", category: "Household", carbon: "12g CO2", water: "0.5 litres", ecoScore: 92, recycling: "100% Recyclable Metal", impact: "Low carbon per use over 1000+ uses", alternative: "Bamboo Insulated Flask", isEcoFriendly: true },
  "1003": { id: "1003", name: "Organic Cotton T-Shirt", category: "Clothing", carbon: "350g CO2", water: "250 litres", ecoScore: 85, recycling: "Textile Bio-Compostable", impact: "91% less water than raw cotton", alternative: "Hemp Fabric Apparel", isEcoFriendly: true },
  "1006": { id: "1006", name: "Bamboo Toothbrush", category: "Household", carbon: "18g CO2", water: "0.8 litres", ecoScore: 94, recycling: "Biodegradable Handle", impact: "100% compostable handle", alternative: "Miswak Natural Branch", isEcoFriendly: true },
  "1007": { id: "1007", name: "AA Alkaline Battery", category: "Electronics", carbon: "180g CO2", water: "15 litres", ecoScore: 28, recycling: "Hazardous E-Waste", impact: "Leaches heavy metals in trash", alternative: "USB Rechargeable Li-Ion Batteries", isEcoFriendly: false }
};

let dbConnected = false;

mongoose.connect(MONGO_URI)
  .then(() => {
    console.log('Connected to MongoDB Database');
    dbConnected = true;
  })
  .catch((err) => {
    console.log('MongoDB connection warning:', err.message);
    console.log('API running in local in-memory fallback mode.');
  });

// Helper to compute deterministic grade and environmental decision (display only)
function computeEcoDecision(score) {
  if (score >= 90) return { grade: "A+", decision: "GREEN", recommendation: "EXCELLENT CHOICE" };
  if (score >= 75) return { grade: "A", decision: "GREEN", recommendation: "SUSTAINABLE SELECTION" };
  if (score >= 60) return { grade: "B", decision: "YELLOW", recommendation: "MODERATE BURDEN" };
  if (score >= 45) return { grade: "C", decision: "YELLOW", recommendation: "IMPROVEMENT RECOMMENDED" };
  if (score >= 25) return { grade: "D", decision: "RED", recommendation: "HIGH ENVIRONMENTAL BURDEN" };
  return { grade: "E", decision: "RED", recommendation: "CRITICAL ENVIRONMENTAL FOOTPRINT" };
}

// Health check endpoints
app.get('/', (req, res) => {
  res.json({
    status: "online",
    system: "Eco Mind AI Backend Server",
    database: dbConnected ? "MongoDB Connected" : "Local Standalone Fallback",
    architecture: "Unidirectional: RFID -> Arduino -> HC-05 -> Mobile App -> Backend -> Gemini AI -> Result Screen",
    endpoints: [
      "GET /health",
      "GET /products",
      "GET /product/:id",
      "GET /api/products/rfid/:uid",
      "POST /product",
      "PUT /product/:id",
      "POST /sync-database",
      "POST /sensor-readings"
    ]
  });
});

app.get('/health', (req, res) => {
  res.json({
    status: "OK",
    server: "EcoMind Multi-Mobile REST Backend",
    database: dbConnected ? "MongoDB" : "InMemory",
    activeProducts: Object.keys(fallbackProducts).length
  });
});

// GET /api/products/rfid/:uid and GET /product/:id - Fetch product by RFID Tag UID
const handleProductFetch = async (req, res) => {
  try {
    const rawId = req.params.uid || req.params.id;
    if (!rawId || typeof rawId !== 'string') {
      return res.status(400).json({ error: "Invalid product ID format." });
    }
    const id = rawId.trim().toUpperCase();

    let productData = null;

    if (dbConnected) {
      const product = await Product.findOne({ id });
      if (product) {
        productData = {
          id: product.id,
          name: product.name,
          category: product.category,
          carbon: product.carbon,
          water: product.water,
          ecoScore: product.ecoScore,
          recycling: product.recycling,
          impact: product.impact,
          alternative: product.alternative,
          isEcoFriendly: product.isEcoFriendly
        };
      }
    }

    if (!productData && fallbackProducts[id]) {
      productData = { ...fallbackProducts[id] };
    }

    if (productData) {
      const { grade, decision, recommendation } = computeEcoDecision(productData.ecoScore);
      return res.json({
        ...productData,
        eco_score: productData.ecoScore,
        grade: grade,
        decision: decision, // Display data only: GREEN, YELLOW, or RED
        recommendation: recommendation,
        positive_factors: [
          productData.isEcoFriendly ? "Low lifecycle carbon intensity" : "High potential for closed-loop recovery",
          `Recycling Category: ${productData.recycling}`
        ],
        key_impact_drivers: [
          `Embodied Carbon: ${productData.carbon}`,
          `Virtual Water Footprint: ${productData.water}`
        ],
        disposal_guidance: `Sort into ${productData.recycling} channel. Avoid mixed general waste.`
      });
    }

    return res.status(404).json({ error: `Product UID ${id} not found in database.` });
  } catch (err) {
    console.error("Product fetch error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
};

app.get('/api/products/rfid/:uid', handleProductFetch);
app.get('/product/:id', handleProductFetch);


// GET /products - List all products
app.get('/products', async (req, res) => {
  try {
    if (dbConnected) {
      const products = await Product.find({});
      return res.json(products);
    }
    return res.json(Object.values(fallbackProducts));
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch products" });
  }
});

// In-memory sensor readings cache
const sensorReadingsDb = [];

// POST /product - Create or update product
app.post('/product', async (req, res) => {
  try {
    const p = req.body;
    if (!p || !p.name) {
      return res.status(400).json({ error: "Product payload requires at least a name." });
    }
    const id = (p.id || `PROD_${Date.now()}`).toString().trim().toUpperCase();
    const productRecord = {
      id,
      name: p.name,
      category: p.category || "General",
      carbon: p.carbon || "100g CO2",
      water: p.water || "5 litres",
      ecoScore: typeof p.ecoScore === 'number' ? p.ecoScore : 65,
      recycling: p.recycling || "Standard Recycling",
      impact: p.impact || "Standard lifecycle impact",
      alternative: p.alternative || "Eco-friendly alternative",
      isEcoFriendly: p.isEcoFriendly !== undefined ? Boolean(p.isEcoFriendly) : (p.ecoScore >= 60)
    };

    fallbackProducts[id] = productRecord;

    if (dbConnected) {
      await Product.findOneAndUpdate({ id }, productRecord, { upsert: true, new: true });
    }

    const { grade, decision, recommendation } = computeEcoDecision(productRecord.ecoScore);
    return res.json({
      ...productRecord,
      grade,
      decision,
      recommendation
    });
  } catch (err) {
    console.error("POST /product error:", err);
    res.status(500).json({ error: err.message });
  }
});

// PUT /product/:id - Update product by id
app.put('/product/:id', async (req, res) => {
  try {
    const id = req.params.id.trim().toUpperCase();
    const p = req.body;
    const existing = fallbackProducts[id] || {};
    const updated = {
      ...existing,
      ...p,
      id
    };
    fallbackProducts[id] = updated;

    if (dbConnected) {
      await Product.findOneAndUpdate({ id }, updated, { upsert: true, new: true });
    }

    res.json(updated);
  } catch (err) {
    console.error("PUT /product/:id error:", err);
    res.status(500).json({ error: err.message });
  }
});

// POST /sync-database - Sync full Room DB (products and sensor readings)
app.post('/sync-database', async (req, res) => {
  try {
    const { products, sensorReadings } = req.body || {};
    let syncedProductsCount = 0;
    let syncedSensorsCount = 0;

    if (products && Array.isArray(products)) {
      for (const p of products) {
        if (!p.id) continue;
        const id = p.id.toString().trim().toUpperCase();
        fallbackProducts[id] = {
          id,
          name: p.name || "Product",
          category: p.category || "General",
          carbon: p.carbon || "100g CO2",
          water: p.water || "5 litres",
          ecoScore: p.ecoScore || p.eco_score || 65,
          recycling: p.recycling || "Standard Recycling",
          impact: p.impact || "",
          alternative: p.alternative || "",
          isEcoFriendly: p.isEcoFriendly !== undefined ? Boolean(p.isEcoFriendly) : true
        };
        syncedProductsCount++;

        if (dbConnected) {
          try {
            await Product.findOneAndUpdate({ id }, fallbackProducts[id], { upsert: true });
          } catch (dbErr) {
            console.warn("MongoDB sync item warning:", dbErr.message);
          }
        }
      }
    }

    if (sensorReadings && Array.isArray(sensorReadings)) {
      sensorReadingsDb.push(...sensorReadings);
      syncedSensorsCount = sensorReadings.length;
    }

    res.json({
      status: "success",
      message: `Full Room DB synced successfully to backend server.`,
      syncedProducts: syncedProductsCount,
      syncedSensors: syncedSensorsCount,
      totalProductsInServer: Object.keys(fallbackProducts).length,
      timestamp: Date.now()
    });
  } catch (err) {
    console.error("POST /sync-database error:", err);
    res.status(500).json({ error: "Failed to sync database: " + err.message });
  }
});

// POST /sensor-readings - Batch or single sensor reading upload
app.post('/sensor-readings', (req, res) => {
  try {
    const body = req.body;
    if (Array.isArray(body)) {
      sensorReadingsDb.push(...body);
      return res.json({ status: "success", count: body.length, total: sensorReadingsDb.length });
    } else if (body && typeof body === 'object') {
      sensorReadingsDb.push(body);
      return res.json({ status: "success", count: 1, total: sensorReadingsDb.length });
    }
    res.status(400).json({ error: "Invalid sensor readings format." });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /sensor-readings - Retrieve stored sensor readings
app.get('/sensor-readings', (req, res) => {
  res.json(sensorReadingsDb.slice(-100));
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Eco Mind AI REST Server listening on port ${PORT} (0.0.0.0)`);
});

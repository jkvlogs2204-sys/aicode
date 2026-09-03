const mongoose = require('mongoose');
require('dotenv').config();
const Product = require('./models/Product');

const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/ecomind';

const sampleProducts = [
  {
    id: "1001",
    name: "Plastic Water Bottle",
    category: "Plastic",
    carbon: "82g CO2",
    water: "3 litres",
    ecoScore: 45,
    recycling: "Recyclable (PET 1)",
    impact: "High single-use plastic pollution in oceans and long breakdown timeline (450+ years).",
    alternative: "Reusable Stainless Steel Bottle",
    isEcoFriendly: false
  },
  {
    id: "1002",
    name: "Reusable Steel Bottle",
    category: "Household",
    carbon: "12g CO2",
    water: "0.5 litres",
    ecoScore: 92,
    recycling: "100% Recyclable Metal",
    impact: "Extremely low per-use carbon footprint over 1,000+ lifetime uses.",
    alternative: "Bamboo Insulated Flask",
    isEcoFriendly: true
  },
  {
    id: "1003",
    name: "Organic Cotton T-Shirt",
    category: "Clothing",
    carbon: "350g CO2",
    water: "250 litres",
    ecoScore: 85,
    recycling: "Textile Bio-Compostable",
    impact: "Uses 91% less water than conventional non-organic cotton and zero synthetic pesticides.",
    alternative: "Hemp Fabric Apparel",
    isEcoFriendly: true
  },
  {
    id: "1004",
    name: "Synthetic Polyester Jacket",
    category: "Clothing",
    carbon: "1200g CO2",
    water: "850 litres",
    ecoScore: 38,
    recycling: "Difficult / Microplastic shedder",
    impact: "Sheds harmful microplastics in every wash and relies on petroleum manufacturing.",
    alternative: "Recycled Wool Coat or Organic Linen",
    isEcoFriendly: false
  },
  {
    id: "1005",
    name: "Single-Use Plastic Straw",
    category: "Plastic",
    carbon: "15g CO2",
    water: "1.2 litres",
    ecoScore: 18,
    recycling: "Non-recyclable due to small size",
    impact: "Frequently ends up in marine environments injuring wildlife.",
    alternative: "Bamboo or Wheat Straws",
    isEcoFriendly: false
  },
  {
    id: "1006",
    name: "Bamboo Toothbrush",
    category: "Household",
    carbon: "18g CO2",
    water: "0.8 litres",
    ecoScore: 94,
    recycling: "Biodegradable Handle / Nylon bristles",
    impact: "100% compostable handle prevents plastic stem accumulation in landfills.",
    alternative: "Miswak Natural Branch",
    isEcoFriendly: true
  },
  {
    id: "1007",
    name: "AA Alkaline Battery",
    category: "Electronics",
    carbon: "180g CO2",
    water: "15 litres",
    ecoScore: 28,
    recycling: "Hazardous E-Waste Dropoff Only",
    impact: "Leaches heavy metals like zinc and manganese if thrown into regular municipal trash.",
    alternative: "USB Rechargeable Li-Ion Batteries",
    isEcoFriendly: false
  },
  {
    id: "1008",
    name: "USB Rechargeable Battery",
    category: "Electronics",
    carbon: "45g CO2",
    water: "2 litres",
    ecoScore: 88,
    recycling: "E-Waste Battery Recycling Center",
    impact: "Replaces over 500 single-use alkaline batteries, saving significant mining raw resources.",
    alternative: "Solar Charged Battery Pack",
    isEcoFriendly: true
  },
  {
    id: "1009",
    name: "Aluminium Soda Can",
    category: "Food",
    carbon: "170g CO2",
    water: "4 litres",
    ecoScore: 78,
    recycling: "Infinitely Recyclable Metal",
    impact: "Recycling saves 95% of the energy needed to produce new raw bauxite aluminium.",
    alternative: "Refillable Glass Bottle",
    isEcoFriendly: true
  },
  {
    id: "1010",
    name: "Disposable Foam Coffee Cup",
    category: "Plastic",
    carbon: "95g CO2",
    water: "5 litres",
    ecoScore: 22,
    recycling: "Non-recyclable Styrofoam",
    impact: "Contains toxic styrene monomers that do not degrade naturally for centuries.",
    alternative: "Ceramic Mug or Glass KeepCup",
    isEcoFriendly: false
  },
  {
    id: "1011",
    name: "Ceramic Coffee Mug",
    category: "Household",
    carbon: "14g CO2",
    water: "0.4 litres",
    ecoScore: 91,
    recycling: "Ceramic / Masonry Fill",
    impact: "Infinite indoor daily reuses eliminate thousands of single-use paper & foam cups.",
    alternative: "Stainless Steel Travel Tumbler",
    isEcoFriendly: true
  },
  {
    id: "1012",
    name: "Incandescent Light Bulb 60W",
    category: "Electronics",
    carbon: "850g CO2",
    water: "12 litres",
    ecoScore: 30,
    recycling: "Standard Trash",
    impact: "Wastes 90% of electrical energy as heat rather than light.",
    alternative: "Energy Efficient LED Bulb",
    isEcoFriendly: false
  },
  {
    id: "1013",
    name: "Energy Efficient LED Bulb",
    category: "Electronics",
    carbon: "110g CO2",
    water: "1.5 litres",
    ecoScore: 95,
    recycling: "E-Waste Recycling",
    impact: "Uses 80% less electricity and lasts up to 25,000 operational hours.",
    alternative: "Smart Solar Daylighting",
    isEcoFriendly: true
  },
  {
    id: "1014",
    name: "Processed Fast Food Burger",
    category: "Food",
    carbon: "2800g CO2",
    water: "1800 litres",
    ecoScore: 32,
    recycling: "Compostable Wrapper",
    impact: "Intensive livestock farming leads to methane emissions and deforestation.",
    alternative: "Plant-Based Mushroom & Lentil Patty",
    isEcoFriendly: false
  },
  {
    id: "1015",
    name: "Plant-Based Organic Salad",
    category: "Food",
    carbon: "120g CO2",
    water: "45 litres",
    ecoScore: 96,
    recycling: "100% Organic Compost",
    impact: "Minimal methane footprint and promotes healthy soil nitrogen fixation.",
    alternative: "Local Seasonal Hydroponic Produce",
    isEcoFriendly: true
  },
  {
    id: "1016",
    name: "Cardboard Shipping Box",
    category: "Household",
    carbon: "60g CO2",
    water: "8 litres",
    ecoScore: 82,
    recycling: "Paper/Cardboard Bin",
    impact: "Easily recycled 7+ times or composted at home.",
    alternative: "Biodegradable Mycelium Packaging",
    isEcoFriendly: true
  },
  {
    id: "1017",
    name: "Synthetic Foam Kitchen Sponge",
    category: "Household",
    carbon: "70g CO2",
    water: "3.5 litres",
    ecoScore: 35,
    recycling: "Non-recyclable Plastic Foam",
    impact: "Releases microplastic fragments into municipal wastewater stream with every dish scrub.",
    alternative: "Natural Plant-Based Loofah Sponge",
    isEcoFriendly: false
  },
  {
    id: "1018",
    name: "Natural Loofah Sponge",
    category: "Household",
    carbon: "8g CO2",
    water: "0.6 litres",
    ecoScore: 97,
    recycling: "100% Home Compostable",
    impact: "Made directly from dried gourd plants without synthetic binders.",
    alternative: "Wooden Dish Brush with Tampico Fiber",
    isEcoFriendly: true
  },
  {
    id: "1019",
    name: "Flagship Electronic Smartphone",
    category: "Electronics",
    carbon: "70,000g CO2",
    water: "13,000 litres",
    ecoScore: 40,
    recycling: "Specialized E-Waste Recovery",
    impact: "Mining rare earth metals creates mining toxic runoff and intensive refining carbon energy.",
    alternative: "Modular Repairable Phone / Refurbished Device",
    isEcoFriendly: false
  },
  {
    id: "1020",
    name: "Recycled Paper Notebook",
    category: "Household",
    carbon: "40g CO2",
    water: "6 litres",
    ecoScore: 89,
    recycling: "Standard Paper Recycling",
    impact: "Prevents virgin tree harvesting and saves 70% energy compared to raw pulp paper.",
    alternative: "Reusable Digital E-Ink Writing Pad",
    isEcoFriendly: true
  }
];

async function seedDB() {
  try {
    await mongoose.connect(MONGO_URI);
    console.log('Connected to MongoDB...');
    await Product.deleteMany({});
    console.log('Cleared old products...');
    await Product.insertMany(sampleProducts);
    console.log(`Successfully seeded ${sampleProducts.length} products!`);
    process.exit(0);
  } catch (err) {
    console.error('Seeding error:', err);
    process.exit(1);
  }
}

seedDB();

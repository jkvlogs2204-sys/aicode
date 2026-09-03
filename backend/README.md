# Eco Mind AI - Node.js Backend & Architecture Guide

This is the backend REST API server for the **Eco Mind AI** Environmental Decision-Making System.

## Unidirectional Communication Architecture

The hardware-to-app data flow is strictly one-way. There is **no reverse communication** from the mobile application back to the Arduino for environmental decisions.

```
                    ┌─────────────────┐
                    │   RFID PRODUCT   │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │  MFRC522 RFID   │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │   ARDUINO UNO   │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │      HC-05      │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │   MOBILE APP    │
                    └────────┬────────┘
                             ↓
                    ┌─────────────────┐
                    │   BACKEND API   │
                    └────────┬────────┘
                             ↓
              ┌────────────────────────────┐
              │  ECO SCORE + GEMINI AI     │
              └─────────────┬──────────────┘
                            ↓
                    ┌─────────────────┐
                    │  RESULT SCREEN  │
                    └─────────────────┘
```

> **Important**: The environmental decision terms (`GREEN`, `YELLOW`, `RED`) exist exclusively in the software layer (Backend & Mobile App Result Screen) as Life Cycle Assessment (LCA) classifications. They are **NOT** Bluetooth commands, and the Arduino does not receive commands or actuate servos/LEDs based on Eco Scores.

## Requirements
- Node.js (v16 or higher)
- MongoDB (optional local service or MongoDB Atlas; in-memory fallback included)

## Setup Steps

1. **Install Dependencies**
   ```bash
   cd backend
   npm install
   ```

2. **Configure Environment Variables**
   Create a `.env` file in the `backend/` directory:
   ```env
   PORT=3000
   MONGO_URI=mongodb://localhost:27017/ecomind
   ```

3. **Seed Database with Sample Products**
   ```bash
   npm run seed
   ```

4. **Start REST API Server**
   ```bash
   npm start
   ```

5. **API Endpoints**
   - Health Check: `GET http://localhost:3000/`
   - Fetch by RFID UID: `GET http://localhost:3000/api/products/rfid/:uid`
   - Legacy Product Route: `GET http://localhost:3000/product/:id`
   - List All Products: `GET http://localhost:3000/products`

Sample Response for `GET http://localhost:3000/api/products/rfid/1001`:
```json
{
  "id": "1001",
  "name": "Plastic Water Bottle",
  "category": "Plastic",
  "carbon": "82g CO2",
  "water": "3 litres",
  "ecoScore": 45,
  "eco_score": 45,
  "grade": "C",
  "decision": "YELLOW",
  "recommendation": "IMPROVEMENT RECOMMENDED",
  "recycling": "Recyclable (PET 1)",
  "impact": "High single-use plastic waste",
  "alternative": "Reusable Steel Bottle",
  "isEcoFriendly": false,
  "positive_factors": [
    "Recycling Category: Recyclable (PET 1)"
  ],
  "key_impact_drivers": [
    "Embodied Carbon: 82g CO2",
    "Virtual Water Footprint: 3 litres"
  ],
  "disposal_guidance": "Sort into Recyclable (PET 1) channel. Avoid mixed general waste."
}
```


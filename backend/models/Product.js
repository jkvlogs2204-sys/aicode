const mongoose = require('mongoose');

const productSchema = new mongoose.Schema({
  id: { type: String, required: true, unique: true },
  name: { type: String, required: true },
  category: { type: String, required: true },
  carbon: { type: String, required: true },
  water: { type: String, required: true },
  ecoScore: { type: Number, required: true },
  recycling: { type: String, required: true },
  impact: { type: String, required: true },
  alternative: { type: String, required: true },
  isEcoFriendly: { type: Boolean, default: false }
}, { timestamps: true });

module.exports = mongoose.model('Product', productSchema);

const mongoose = require('mongoose');

const translationSchema = new mongoose.Schema({
    lang: String,
    text: String
}, { _id: false });

const messageSchema = new mongoose.Schema({
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Room',
        required: true,
        index: true
    },
    sender: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    type: {
        type: String,
        enum: ['text', 'image', 'voice', 'system'],
        default: 'text'
    },
    text: {
        type: String,
        default: ''
    },
    originalLang: {
        type: String,
        default: 'en'
    },
    translations: [translationSchema],
    mediaUrl: {
        type: String,
        default: null
    },
    mediaDuration: {
        type: Number,   // seconds, for voice
        default: null
    },
    // Delivery status
    deliveredTo: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    }],
    seenBy: [{
        userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        seenAt: { type: Date }
    }],
    isDeleted: {
        type: Boolean,
        default: false
    }
}, {
    timestamps: true
});

// Get translation for a given language, fallback to original text
messageSchema.methods.getTextForLang = function (lang) {
    if (!lang || lang === this.originalLang) return this.text;
    const t = this.translations.find(t => t.lang === lang);
    return t ? t.text : this.text;
};

module.exports = mongoose.model('Message', messageSchema);

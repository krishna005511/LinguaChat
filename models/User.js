const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        minlength: 3,
        maxlength: 30
    },
    email: {
        type: String,
        required: true,
        unique: true,
        lowercase: true,
        trim: true
    },
    passwordHash: {
        type: String,
        required: true
    },
    displayName: {
        type: String,
        trim: true,
        maxlength: 50
    },
    avatarUrl: {
        type: String,
        default: null
    },
    preferredLanguage: {
        type: String,
        default: 'en'
    },
    bio: {
        type: String,
        maxlength: 150,
        default: ''
    },
    isVerified: {
        type: Boolean,
        default: false
    },
    isOnline: {
        type: Boolean,
        default: false
    },
    lastSeen: {
        type: Date,
        default: Date.now
    },
    socketId: {
        type: String,
        default: null
    },
    // OTP fields
    otp: {
        code: { type: String, default: null },
        expiresAt: { type: Date, default: null }
    },
    // FCM token for push notifications (Phase 8)
    fcmToken: {
        type: String,
        default: null
    }
}, {
    timestamps: true
});

// Hash password before saving
userSchema.pre('save', async function (next) {
    if (!this.isModified('passwordHash')) return next();
    this.passwordHash = await bcrypt.hash(this.passwordHash, 12);
    next();
});

// Compare password
userSchema.methods.comparePassword = async function (plainPassword) {
    return bcrypt.compare(plainPassword, this.passwordHash);
};

// Never expose sensitive fields
userSchema.methods.toPublicJSON = function () {
    return {
        id: this._id,
        username: this.username,
        email: this.email,
        displayName: this.displayName || this.username,
        avatarUrl: this.avatarUrl,
        preferredLanguage: this.preferredLanguage,
        bio: this.bio,
        isVerified: this.isVerified,
        isOnline: this.isOnline,
        lastSeen: this.lastSeen
    };
};

module.exports = mongoose.model('User', userSchema);

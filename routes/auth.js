const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const bcrypt = require('bcryptjs');

const User = require('../models/User');
const { generateOTP, getOTPExpiry, sendOTPEmail } = require('../utils/emailOTP');

// Rate limiter for OTP sending (max 3 per hour per IP)
const otpLimiter = rateLimit({
    windowMs: 60 * 60 * 1000,
    max: 3,
    message: { message: 'Too many OTP requests, please try again later' }
});

// Rate limiter for login attempts
const loginLimiter = rateLimit({
    windowMs: 15 * 60 * 1000,
    max: 10,
    message: { message: 'Too many login attempts, please try again later' }
});

function generateToken(userId) {
    return jwt.sign(
        { userId },
        process.env.JWT_SECRET,
        { expiresIn: process.env.JWT_EXPIRES_IN || '30d' }
    );
}

// ─────────────────────────────────────────────
// POST /api/auth/register
// Step 1: Create account (unverified), send OTP
// ─────────────────────────────────────────────
router.post('/register', otpLimiter, [
    body('username').trim().isLength({ min: 3, max: 30 }).matches(/^[a-zA-Z0-9_]+$/),
    body('email').isEmail().normalizeEmail(),
    body('password').isLength({ min: 6 })
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ message: 'Validation failed', errors: errors.array() });
    }

    const { username, email, password, displayName } = req.body;

    try {
        // Check if username or email already exists
        const existing = await User.findOne({ $or: [{ email }, { username }] });
        if (existing) {
            if (existing.email === email) {
                return res.status(409).json({ message: 'Email already registered' });
            }
            return res.status(409).json({ message: 'Username already taken' });
        }

        const otp = generateOTP();
        const otpExpiry = getOTPExpiry();

        // HASH PASSWORD (FIX)
        const hashedPassword = await bcrypt.hash(password, 10);

        const user = new User({
            username,
            email,
            passwordHash: hashedPassword,
            displayName: displayName || username,
            isVerified: false,
            otp: { code: otp, expiresAt: otpExpiry }
        });

        await user.save();
        await sendOTPEmail(email, otp, username);

        res.status(201).json({
            message: 'Account created. Please verify your email.',
            userId: user._id
        });
    } catch (err) {
        console.error('Register error:', err);
        res.status(500).json({ message: 'Server error during registration' });
    }
});

// ─────────────────────────────────────────────
// POST /api/auth/verify-otp
// Step 2: Verify email OTP → get JWT
// ─────────────────────────────────────────────
router.post('/verify-otp', async (req, res) => {
    const { userId, otp } = req.body;

    if (!userId || !otp) {
        return res.status(400).json({ message: 'userId and otp are required' });
    }

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        if (!user.otp.code || !user.otp.expiresAt) {
            return res.status(400).json({ message: 'No OTP pending for this account' });
        }

        if (new Date() > user.otp.expiresAt) {
            return res.status(400).json({ message: 'OTP expired. Please request a new one.' });
        }

        if (user.otp.code !== otp) {
            return res.status(400).json({ message: 'Incorrect OTP' });
        }

        user.isVerified = true;
        user.otp = { code: null, expiresAt: null };
        await user.save();

        const token = generateToken(user._id);

        res.json({
            message: 'Email verified successfully',
            token,
            user: user.toPublicJSON()
        });
    } catch (err) {
        console.error('Verify OTP error:', err);
        res.status(500).json({ message: 'Server error during verification' });
    }
});

// ─────────────────────────────────────────────
// POST /api/auth/resend-otp
// ─────────────────────────────────────────────
router.post('/resend-otp', otpLimiter, async (req, res) => {
    const { userId } = req.body;

    if (!userId) {
        return res.status(400).json({ message: 'userId is required' });
    }

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        if (user.isVerified) {
            return res.status(400).json({ message: 'Account already verified' });
        }

        const otp = generateOTP();
        user.otp = { code: otp, expiresAt: getOTPExpiry() };
        await user.save();

        await sendOTPEmail(user.email, otp, user.username);

        res.json({ message: 'OTP resent successfully' });
    } catch (err) {
        console.error('Resend OTP error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/auth/login
// ─────────────────────────────────────────────
router.post('/login', loginLimiter, [
    body('email').isEmail().normalizeEmail(),
    body('password').notEmpty()
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ message: 'Invalid email or password format' });
    }

    const { email, password } = req.body;

    try {
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(401).json({ message: 'Invalid email or password' });
        }

        const isMatch = await user.comparePassword(password);
        if (!isMatch) {
            return res.status(401).json({ message: 'Invalid email or password' });
        }

        if (!user.isVerified) {
            return res.status(403).json({
                message: 'Email not verified. Please check your inbox.',
                userId: user._id,
                needsVerification: true
            });
        }

        const token = generateToken(user._id);

        res.json({
            message: 'Login successful',
            token,
            user: user.toPublicJSON()
        });
    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ message: 'Server error during login' });
    }
});

module.exports = router;
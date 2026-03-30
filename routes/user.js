const express = require('express');
const router = express.Router();
const { body, validationResult } = require('express-validator');

const User = require('../models/User');
const authMiddleware = require('../middleware/auth');

// All routes require auth
router.use(authMiddleware);

// ─────────────────────────────────────────────
// GET /api/user/me
// Get current user profile
// ─────────────────────────────────────────────
router.get('/me', (req, res) => {
    res.json({ user: req.user.toPublicJSON() });
});

// ─────────────────────────────────────────────
// PUT /api/user/me
// Update current user profile
// ─────────────────────────────────────────────
router.put('/me', [
    body('displayName').optional().trim().isLength({ max: 50 }),
    body('bio').optional().trim().isLength({ max: 150 }),
    body('preferredLanguage').optional().isLength({ min: 2, max: 5 }),
    body('avatarUrl').optional().isURL()
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ message: 'Validation failed', errors: errors.array() });
    }

    const allowedFields = ['displayName', 'bio', 'preferredLanguage', 'avatarUrl'];
    const updates = {};
    allowedFields.forEach(field => {
        if (req.body[field] !== undefined) {
            updates[field] = req.body[field];
        }
    });

    try {
        const user = await User.findByIdAndUpdate(
            req.user._id,
            { $set: updates },
            { new: true, runValidators: true }
        );
        res.json({ message: 'Profile updated', user: user.toPublicJSON() });
    } catch (err) {
        console.error('Update profile error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// GET /api/user/search?q=username
// Search users by username or displayName
// ─────────────────────────────────────────────
router.get('/search', async (req, res) => {
    const query = req.query.q;
    if (!query || query.length < 2) {
        return res.status(400).json({ message: 'Query must be at least 2 characters' });
    }

    try {
        const users = await User.find({
            $or: [
                { username: { $regex: query, $options: 'i' } },
                { displayName: { $regex: query, $options: 'i' } }
            ],
            _id: { $ne: req.user._id },   // exclude self
            isVerified: true
        }).limit(20).select('username displayName avatarUrl bio isOnline lastSeen preferredLanguage');

        res.json({ users });
    } catch (err) {
        console.error('Search error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// GET /api/user/:userId
// Get any user's public profile
// ─────────────────────────────────────────────
router.get('/:userId', async (req, res) => {
    try {
        const user = await User.findById(req.params.userId)
            .select('username displayName avatarUrl bio isOnline lastSeen preferredLanguage');

        if (!user) {
            return res.status(404).json({ message: 'User not found' });
        }

        res.json({ user });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/user/fcm-token
// Update FCM push notification token (Phase 8)
// ─────────────────────────────────────────────
router.post('/fcm-token', async (req, res) => {
    const { fcmToken } = req.body;
    if (!fcmToken) {
        return res.status(400).json({ message: 'fcmToken is required' });
    }

    try {
        await User.findByIdAndUpdate(req.user._id, { fcmToken });
        res.json({ message: 'FCM token updated' });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

module.exports = router;

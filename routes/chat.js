const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

const Room = require('../models/Room');
const Message = require('../models/Message');
const User = require('../models/User');
const authMiddleware = require('../middleware/auth');

router.use(authMiddleware);

// ─────────────────────────────────────────────
// GET /api/chat/rooms
// Get all rooms for current user (chat list)
// ─────────────────────────────────────────────
router.get('/rooms', async (req, res) => {
    try {
        const rooms = await Room.find({ members: req.user._id })
            .sort({ lastActivity: -1 })
            .populate('members', 'username displayName avatarUrl isOnline lastSeen preferredLanguage')
            .populate({
                path: 'lastMessage',
                select: 'text type originalLang translations sender createdAt isDeleted'
            });

        res.json({ rooms });
    } catch (err) {
        console.error('Get rooms error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/chat/rooms/direct
// Get or create a direct (1-to-1) room
// ─────────────────────────────────────────────
router.post('/rooms/direct', async (req, res) => {
    const { targetUserId } = req.body;

    if (!targetUserId) {
        return res.status(400).json({ message: 'targetUserId is required' });
    }

    if (targetUserId === req.user._id.toString()) {
        return res.status(400).json({ message: 'Cannot create chat with yourself' });
    }

    try {
        // Check target user exists
        const targetUser = await User.findById(targetUserId);
        if (!targetUser) {
            return res.status(404).json({ message: 'User not found' });
        }

        // Find existing direct room with exactly these two members
        let room = await Room.findOne({
            type: 'direct',
            members: { $all: [req.user._id, targetUserId], $size: 2 }
        }).populate('members', 'username displayName avatarUrl isOnline lastSeen preferredLanguage');

        if (!room) {
            room = await Room.create({
                type: 'direct',
                members: [req.user._id, targetUserId]
            });
            room = await room.populate('members', 'username displayName avatarUrl isOnline lastSeen preferredLanguage');
        }

        res.json({ room });
    } catch (err) {
        console.error('Create direct room error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/chat/rooms/group
// Create a group chat
// ─────────────────────────────────────────────
router.post('/rooms/group', async (req, res) => {
    const { name, memberIds } = req.body;

    if (!name || !memberIds || !Array.isArray(memberIds) || memberIds.length < 2) {
        return res.status(400).json({ message: 'name and at least 2 memberIds required' });
    }

    try {
        const allMembers = [...new Set([req.user._id.toString(), ...memberIds])];

        const room = await Room.create({
            type: 'group',
            name,
            members: allMembers,
            admins: [req.user._id],
            createdBy: req.user._id
        });

        const populated = await room.populate('members', 'username displayName avatarUrl isOnline preferredLanguage');

        res.status(201).json({ room: populated });
    } catch (err) {
        console.error('Create group error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// GET /api/chat/rooms/:roomId/messages
// Paginated message history
// ─────────────────────────────────────────────
router.get('/rooms/:roomId/messages', async (req, res) => {
    const { roomId } = req.params;
    const limit = parseInt(req.query.limit) || 40;
    const before = req.query.before;   // message _id for pagination cursor

    try {
        // Verify user is a member
        const room = await Room.findOne({ _id: roomId, members: req.user._id });
        if (!room) {
            return res.status(403).json({ message: 'Not a member of this room' });
        }

        const query = { roomId, isDeleted: false };
        if (before) {
            query._id = { $lt: new mongoose.Types.ObjectId(before) };
        }

        const messages = await Message.find(query)
            .sort({ createdAt: -1 })
            .limit(limit)
            .populate('sender', 'username displayName avatarUrl');

        res.json({ messages: messages.reverse() });
    } catch (err) {
        console.error('Get messages error:', err);
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/chat/rooms/:roomId/seen
// Mark all messages in room as seen by current user
// ─────────────────────────────────────────────
router.post('/rooms/:roomId/seen', async (req, res) => {
    const { roomId } = req.params;

    try {
        await Message.updateMany(
            {
                roomId,
                'seenBy.userId': { $ne: req.user._id },
                sender: { $ne: req.user._id }
            },
            {
                $addToSet: { seenBy: { userId: req.user._id, seenAt: new Date() } }
            }
        );

        res.json({ message: 'Marked as seen' });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

// ─────────────────────────────────────────────
// POST /api/chat/rooms/:roomId/members
// Add member to group (admin only)
// ─────────────────────────────────────────────
router.post('/rooms/:roomId/members', async (req, res) => {
    const { userId } = req.body;

    try {
        const room = await Room.findOne({ _id: req.params.roomId, admins: req.user._id });
        if (!room || room.type !== 'group') {
            return res.status(403).json({ message: 'Not authorized' });
        }

        if (room.members.includes(userId)) {
            return res.status(400).json({ message: 'User already in group' });
        }

        room.members.push(userId);
        await room.save();

        res.json({ message: 'Member added' });
    } catch (err) {
        res.status(500).json({ message: 'Server error' });
    }
});

module.exports = router;

const jwt = require('jsonwebtoken');
const User = require('../models/User');
const Room = require('../models/Room');
const Message = require('../models/Message');
const { detectLanguage, translateForMembers } = require('../utils/translation');

function initSocket(io) {

    io.use(async (socket, next) => {
        const token = socket.handshake.auth.token;
        if (!token) return next(new Error('Authentication required'));
        try {
            const decoded = jwt.verify(token, process.env.JWT_SECRET);
            const user = await User.findById(decoded.userId).select('-passwordHash -otp');
            if (!user) return next(new Error('User not found'));
            socket.user = user;
            next();
        } catch (err) {
            next(new Error('Invalid token'));
        }
    });

    io.on('connection', async (socket) => {
        const user = socket.user;
        console.log('[Socket] Connected: ' + user.username + ' (' + socket.id + ')');

        await User.findByIdAndUpdate(user._id, {
            isOnline: true,
            socketId: socket.id,
            lastSeen: new Date()
        });

        const rooms = await Room.find({ members: user._id }).select('_id');
        rooms.forEach(r => socket.join(r._id.toString()));
        socket.broadcast.emit('user_online', { userId: user._id });

        // ── send_message ──────────────────────────────────────────────────────
        socket.on('send_message', async (data, ack) => {
            try {
                const { roomId, text, type, mediaUrl, mediaDuration } = data;
                const msgType = type || 'text';

                if (!roomId) return ack && ack({ error: 'roomId required' });

                const room = await Room.findOne({ _id: roomId, members: user._id })
                    .populate('members', 'preferredLanguage _id');
                if (!room) return ack && ack({ error: 'Room not found' });

                const originalLang = text ? await detectLanguage(text) : 'en';
                const memberLangs = room.members.map(m => m.preferredLanguage).filter(Boolean);
                const translations = text
                    ? await translateForMembers(text, originalLang, memberLangs)
                    : [];

                const message = await Message.create({
                    roomId,
                    sender: user._id,
                    type: msgType,
                    text: text || '',
                    originalLang,
                    translations,
                    mediaUrl: mediaUrl || null,
                    mediaDuration: mediaDuration || null,
                    deliveredTo: [user._id]
                });

                await Room.findByIdAndUpdate(roomId, {
                    lastMessage: message._id,
                    lastActivity: new Date()
                });

                const populated = await message.populate('sender', 'username displayName avatarUrl');
                io.to(roomId).emit('new_message', populated);
                ack && ack({ success: true, messageId: message._id });

            } catch (err) {
                console.error('[Socket] send_message error:', err);
                ack && ack({ error: 'Failed to send message' });
            }
        });

        // ── typing ────────────────────────────────────────────────────────────
        socket.on('typing_start', ({ roomId }) => {
            socket.to(roomId).emit('user_typing', {
                roomId,
                userId: user._id,
                username: user.displayName || user.username
            });
        });

        socket.on('typing_stop', ({ roomId }) => {
            socket.to(roomId).emit('user_stopped_typing', { roomId, userId: user._id });
        });

        // ── seen / delivered ──────────────────────────────────────────────────
        socket.on('mark_seen', async ({ roomId, messageId }) => {
            try {
                await Message.findByIdAndUpdate(messageId, {
                    $addToSet: { seenBy: { userId: user._id, seenAt: new Date() } }
                });
                const msg = await Message.findById(messageId).select('sender roomId');
                if (msg) {
                    const sender = await User.findById(msg.sender).select('socketId');
                    if (sender && sender.socketId) {
                        io.to(sender.socketId).emit('message_seen', {
                            messageId, roomId,
                            seenBy: user._id, seenAt: new Date()
                        });
                    }
                }
            } catch (err) {
                console.error('[Socket] mark_seen error:', err);
            }
        });

        socket.on('mark_delivered', async ({ messageId }) => {
            try {
                await Message.findByIdAndUpdate(messageId, {
                    $addToSet: { deliveredTo: user._id }
                });
                const msg = await Message.findById(messageId).select('sender');
                if (msg) {
                    const sender = await User.findById(msg.sender).select('socketId');
                    if (sender && sender.socketId) {
                        io.to(sender.socketId).emit('message_delivered', {
                            messageId, deliveredTo: user._id
                        });
                    }
                }
            } catch (err) {
                console.error('[Socket] mark_delivered error:', err);
            }
        });

        socket.on('join_room', ({ roomId }) => socket.join(roomId));

        // ── disconnect ────────────────────────────────────────────────────────
        socket.on('disconnect', async () => {
            const now = new Date();
            await User.findByIdAndUpdate(user._id, {
                isOnline: false, socketId: null, lastSeen: now
            });
            socket.broadcast.emit('user_offline', { userId: user._id, lastSeen: now });
        });

        socket.on('ping', () => socket.emit('pong'));
    });
}

module.exports = initSocket;

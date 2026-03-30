require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const mongoose = require('mongoose');
const cors = require('cors');

const authRoutes  = require('./routes/auth');
const userRoutes  = require('./routes/user');
const chatRoutes  = require('./routes/chat');
const mediaRoutes = require('./routes/media');
const initSocket  = require('./socket/socketHandler');

const app    = express();
const server = http.createServer(app);
const io     = new Server(server, {
    cors: { origin: '*', methods: ['GET', 'POST', 'PUT'] }
});

app.use(cors());
app.use(express.json({ limit: '20mb' }));   // large enough for base64 images
app.use(express.urlencoded({ extended: true, limit: '20mb' }));

app.use('/api/auth',  authRoutes);
app.use('/api/user',  userRoutes);
app.use('/api/chat',  chatRoutes);
app.use('/api/media', mediaRoutes);

app.get('/', (req, res) => {
    res.json({ status: 'LinguaChat server running', version: '2.0.0' });
});

initSocket(io);

mongoose.connect(process.env.MONGO_URI)
    .then(() => {
        console.log('MongoDB connected');
        const PORT = process.env.PORT || 3000;
        server.listen(PORT, () => console.log('Server on port ' + PORT));
    })
    .catch(err => {
        console.error('MongoDB error:', err);
        process.exit(1);
    });

module.exports = { io };

const express = require('express');
const router = express.Router();
const https = require('https');
const http = require('http');
const authMiddleware = require('../middleware/auth');

router.use(authMiddleware);

// ─────────────────────────────────────────────
// POST /api/media/upload
// Upload base64 image or audio to Cloudinary
// Body: { data: "base64string", type: "image"|"voice", roomId: "..." }
// ─────────────────────────────────────────────
router.post('/upload', async (req, res) => {
    const { data, type } = req.body;

    if (!data || !type) {
        return res.status(400).json({ message: 'data and type are required' });
    }
    if (!['image', 'voice'].includes(type)) {
        return res.status(400).json({ message: 'type must be image or voice' });
    }

    try {
        const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
        const uploadPreset = process.env.CLOUDINARY_UPLOAD_PRESET;

        if (!cloudName || !uploadPreset) {
            return res.status(500).json({ message: 'Cloudinary not configured' });
        }

        const resourceType = type === 'voice' ? 'video' : 'image';   // Cloudinary uses "video" for audio
        const folder = type === 'voice' ? 'linguachat/voice' : 'linguachat/images';

        const payload = JSON.stringify({
            file: data,
            upload_preset: uploadPreset,
            folder,
            resource_type: resourceType
        });

        const result = await postJSON(
            `https://api.cloudinary.com/v1_1/${cloudName}/${resourceType}/upload`,
            payload
        );

        if (result.secure_url) {
            res.json({
                url: result.secure_url,
                publicId: result.public_id,
                duration: result.duration || null     // for audio
            });
        } else {
            res.status(500).json({ message: 'Cloudinary upload failed', detail: result });
        }
    } catch (err) {
        console.error('Media upload error:', err);
        res.status(500).json({ message: 'Upload failed' });
    }
});

function postJSON(url, body) {
    return new Promise((resolve, reject) => {
        const parsed = new URL(url);
        const lib = parsed.protocol === 'https:' ? https : http;
        const options = {
            hostname: parsed.hostname,
            port: parsed.port || 443,
            path: parsed.pathname,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(body)
            },
            timeout: 30000
        };
        const req = lib.request(options, resp => {
            let raw = '';
            resp.on('data', c => raw += c);
            resp.on('end', () => {
                try { resolve(JSON.parse(raw)); }
                catch (e) { reject(new Error('Invalid Cloudinary response')); }
            });
        });
        req.on('error', reject);
        req.on('timeout', () => { req.destroy(); reject(new Error('Upload timed out')); });
        req.write(body);
        req.end();
    });
}

module.exports = router;

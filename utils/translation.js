const https = require('https');
const http = require('http');

// LibreTranslate public instance — free, no API key needed for basic use
// If rate limited, swap to: https://translate.argosopentech.com
const LIBRETRANSLATE_URL = process.env.LIBRETRANSLATE_URL || 'https://libretranslate.com';

/**
 * Detect language of a text string.
 * Returns ISO language code e.g. "en", "hi", "es"
 */
async function detectLanguage(text) {
    try {
        const result = await postJSON(`${LIBRETRANSLATE_URL}/detect`, {
            q: text.substring(0, 200)   // limit for detection
        });
        if (result && result.length > 0) {
            return result[0].language;
        }
    } catch (err) {
        console.warn('Language detection failed, defaulting to en:', err.message);
    }
    return 'en';
}

/**
 * Translate text from sourceLang to targetLang.
 */
async function translateText(text, sourceLang, targetLang) {
    if (!text || sourceLang === targetLang) return text;

    try {
        const result = await postJSON(`${LIBRETRANSLATE_URL}/translate`, {
            q: text,
            source: sourceLang,
            target: targetLang,
            format: 'text'
        });
        return result.translatedText || text;
    } catch (err) {
        console.warn(`Translation ${sourceLang}->${targetLang} failed:`, err.message);
        return text;   // fallback to original
    }
}

/**
 * Given a message text and its detected language, produce translations
 * for all unique preferred languages of room members.
 *
 * Returns array: [{ lang: 'hi', text: '...' }, ...]
 */
async function translateForMembers(text, sourceLang, memberLangs) {
    const uniqueLangs = [...new Set(memberLangs)].filter(l => l && l !== sourceLang);
    const translations = [];

    await Promise.all(
        uniqueLangs.map(async lang => {
            const translated = await translateText(text, sourceLang, lang);
            translations.push({ lang, text: translated });
        })
    );

    return translations;
}

// ── Internal HTTP helper ──────────────────────────────────────────────────────

function postJSON(url, body) {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify(body);
        const parsed = new URL(url);
        const lib = parsed.protocol === 'https:' ? https : http;

        const options = {
            hostname: parsed.hostname,
            port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
            path: parsed.pathname,
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Content-Length': Buffer.byteLength(data)
            },
            timeout: 8000
        };

        const req = lib.request(options, res => {
            let raw = '';
            res.on('data', chunk => raw += chunk);
            res.on('end', () => {
                try {
                    resolve(JSON.parse(raw));
                } catch (e) {
                    reject(new Error('Invalid JSON from LibreTranslate'));
                }
            });
        });

        req.on('error', reject);
        req.on('timeout', () => {
            req.destroy();
            reject(new Error('LibreTranslate request timed out'));
        });

        req.write(data);
        req.end();
    });
}

module.exports = { detectLanguage, translateText, translateForMembers };

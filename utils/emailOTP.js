const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS   // Use Gmail App Password, not account password
    }
});

/**
 * Generate a 6-digit OTP
 */
function generateOTP() {
    return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * Get OTP expiry date (default 10 minutes from now)
 */
function getOTPExpiry() {
    const minutes = parseInt(process.env.OTP_EXPIRY_MINUTES) || 10;
    return new Date(Date.now() + minutes * 60 * 1000);
}

/**
 * Send OTP email to user
 */
async function sendOTPEmail(toEmail, otp, username) {
    const expiryMinutes = parseInt(process.env.OTP_EXPIRY_MINUTES) || 10;

    const mailOptions = {
        from: `"LinguaChat" <${process.env.EMAIL_USER}>`,
        to: toEmail,
        subject: 'Your LinguaChat verification code',
        html: `
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
                <h2 style="color: #1a73e8;">LinguaChat</h2>
                <p>Hi <strong>${username}</strong>,</p>
                <p>Your verification code is:</p>
                <div style="background: #f1f3f4; border-radius: 8px; padding: 20px; text-align: center; margin: 24px 0;">
                    <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #1a73e8;">${otp}</span>
                </div>
                <p>This code expires in <strong>${expiryMinutes} minutes</strong>.</p>
                <p>If you didn't request this, you can safely ignore this email.</p>
                <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 24px 0;">
                <p style="color: #999; font-size: 12px;">LinguaChat — Chat without language barriers.</p>
            </div>
        `
    };

    await transporter.sendMail(mailOptions);
}

module.exports = { generateOTP, getOTPExpiry, sendOTPEmail };

# Simple AutoBan

A lightweight server-side mod that automatically bans non-whitelisted players by both username AND IP address. Perfect for private Minecraft servers that want bulletproof access control.

## 🔧 Key Features

**Dual Protection:** Instantly bans names + IPs of unauthorized players

**Whitelist Enforcement:** Only configured players can join

**Persistent Bans:** Uses Minecraft's native ban system

**Auto-Saving Config:** All changes save to **config/simpleautoban.json**

## 🛡️ How It Works
When enabled, any non-whitelisted player attempting to join will:

- Be immediately disconnected
- Have their **username permanently banned**
- Have their **IP address permanently banned**

 Whitelisted players enjoy normal access

## 📜 Commands

**Whitelist Management:**

/autoban add <username> - Adds player to whitelist

/autoban remove <username> - Removes player from whitelist

/autoban list - Shows all whitelisted players


**System Control:**

/autoban on - Activates the AutoBan system (off by default)

/autoban off - Deactivates the AutoBan system

/autoban status - Shows current system status


**Ban Customization:**

/autoban setreason <text> - Sets the ban reason

## 💡 Ideal For

- Private friend servers
- Whitelisted communities
- Servers needing IP+name protection
- Preventing brute-force join attempts

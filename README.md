# Krylix - CS:GO Style Kill Notifications

<div align="center">

![Krylix Logo](https://img.shields.io/badge/Krylix-Kill%20Notifications-brightgreen?style=for-the-badge)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![Mod Loaders](https://img.shields.io/badge/Loaders-Fabric%20%7C%20Forge-purple?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Written%20in-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)

**A Minecraft mod that brings CS:GO-style real-time kill notifications with smooth animations and full customization to your server.**

[![Features](#-features)] [![Installation](#-installation)] [![Configuration](#-configuration)] [![Development](#-development)] [![Contributing](#-contributing)]

</div>

## 🌟 Features

### ⚡ Real-time Kill Notifications
- **CS:GO Style**: Professional kill feed design inspired by Counter-Strike
- **Smooth Animations**: Fluid transitions and visual effects
- **Customizable Display**: Full control over appearance and positioning
- **Multi-language Support**: Works with different server languages

### 🎨 Visual Customization
- **Color Schemes**: Choose from predefined themes or create custom colors
- **Animation Speed**: Adjust notification duration and transition speed
- **Position Control**: Place notifications anywhere on screen
- **Font Styling**: Customizable text formatting and sizes

### ⚙️ Advanced Configuration
- **Per-Weapon Settings**: Different styles for different weapons
- **Kill Streak Notifications**: Special effects for multiple kills
- **Sound Effects**: Optional audio feedback for kills
- **Filter Options**: Choose which kills to display

### 🔧 Developer Friendly
- **Kotlin Powered**: Modern, type-safe codebase
- **Multi-Loader Support**: Works on both Fabric and Forge
- **API Integration**: Easy integration with other mods
- **Performance Optimized**: Minimal impact on server performance

## 📋 Requirements

- **Minecraft**: 1.20.1
- **Java**: 17 or higher
- **Mod Loader**: Fabric Loader 0.14.21+ OR Forge 47.0.19+
- **Dependencies**: 
  - Fabric: Fabric API 0.84.0+, Fabric Language Kotlin
  - Forge: KotlinForForge 4.3.0+

## 🚀 Installation

### Fabric Installation

1. **Download the latest Fabric release** from the [Releases page](https://github.com/eliasnvx/Krylix/releases)
2. **Install Fabric Loader** if not already present
3. **Install Fabric Language Kotlin** 
4. **Place the JAR file** in your `mods/` directory
5. **Install Fabric API** (required dependency)
6. **Start your game**

### Forge Installation

1. **Download the latest Forge release** from the [Releases page](https://github.com/eliasnvx/Krylix/releases)
2. **Install Forge** 47.0.19 or higher
3. **Place the JAR file** in your `mods/` directory
4. **Start your game**

## 🏗️ Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/eliasnvx/Krylix.git
cd Krylix

# Build for all platforms
./gradlew build

# Build specific platform
./gradlew :fabric:build
./gradlew :forge:build
```

### Project Structure

```
Krylix/
├── common/              # Shared code between platforms
│   └── src/main/kotlin/
├── fabric/              # Fabric-specific implementation
│   └── src/main/kotlin/
├── forge/               # Forge-specific implementation
│   └── src/main/kotlin/
├── gradle/              # Gradle configuration
└── build.gradle.kts     # Root build configuration
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit your changes**: `git commit -m 'Add amazing feature'`
4. **Push to the branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Lead Developer**: [eliasnvx](https://github.com/eliasnvx)
- **Contributors**: [All contributors](https://github.com/eliasnvx/Krylix/graphs/contributors)
- **Inspiration**: CS:GO kill notification system

---

<div align="center">

**⭐ Star this repository if you find it useful!**

Made with ❤️ and ☕ for the Minecraft community

</div>

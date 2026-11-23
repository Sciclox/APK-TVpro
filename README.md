# 📺 TVPro - Cross-Platform IPTV & Media Center Suite

> Ultra-responsive Android TV and Desktop streaming player engineered with Jetpack Compose, Tauri, and optimized hardware video decoders.

[![Platform](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Windows%20%7C%20Linux-green.svg)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20TV-blue)](https://developer.android.com/jetpack/compose)
[![Tauri](https://img.shields.io/badge/Desktop-Tauri%20%2F%20Rust-orange)](https://tauri.app/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

---

## 🌟 Overview

**TVPro** is an all-in-one entertainment client engineered for both high-end Smart TVs and desktop workstations. It features an ultra-low-latency HLS/DASH streaming pipeline, multi-format EPG (Electronic Program Guide) scheduling, server stream health monitoring, and a D-pad optimized navigation UI.

---

## 🏗️ Architecture

- **Android App (ndroid-app/)**: Native Kotlin application utilizing Jetpack Compose for TV, Leanback integration, and custom ExoPlayer decoders.
- **Desktop Application (src-tauri/)**: Lightweight, memory-efficient desktop runner written in Rust with WebView frontend bindings.
- **Stream Ranker & Extractors**: Autonomous server health evaluation selecting lowest jitter nodes.

---

## 🚀 Getting Started

### Android TV Client
`ash
cd android-app
./gradlew assembleRelease
`

### Desktop Client (Tauri)
`ash
npm install
npm run tauri dev
`

---

## 👤 Author
**Kevin Palacios Pozo** - Software Engineer  
- GitHub: [@Sciclox](https://github.com/Sciclox)

# Clash Android - 项目简历

## 项目概述

**项目名称：** Clash Android  
**项目类型：** Android VPN 代理客户端  
**开发语言：** Kotlin + Rust  
**开源许可：** GPL-3.0  
**GitHub：** [Watfaq/clash-android](https://github.com/Watfaq/clash-android)

## 项目简介

Clash Android 是一款基于 Clash 核心的现代化 Android VPN 代理客户端应用，采用 Kotlin 和 Rust 混合开发架构，提供高性能的网络代理服务。本项目使用最新的 Android 开发技术栈，包括 Jetpack Compose、Material Design 3 等现代化 UI 框架，为用户提供流畅、美观的使用体验。

## 技术栈

### Android 端
- **开发语言：** Kotlin 2.3.0
- **UI 框架：** Jetpack Compose (Compose BOM 2026.01.00)
- **Material Design：** Material 3
- **架构组件：** 
  - AndroidX Core KTX
  - Lifecycle ViewModel Compose
  - Compose Destinations（导航框架）
- **构建工具：** Gradle 8.13.2 (Kotlin DSL)
- **最低支持版本：** Android 6.0 (API 23)
- **目标版本：** Android 14 (API 36)

### Rust 核心
- **核心语言：** Rust
- **FFI 绑定：** UniFFI（Kotlin-Rust 互操作）
- **核心库：** clash-lib（来自 clash-rs 项目）
- **网络协议支持：**
  - TUIC
  - ShadowQuic
  - Shadowsocks
  - TUN（虚拟网卡）
  - WireGuard
- **HTTP 客户端：** Hyper + Rustls
- **异步运行时：** Tokio
- **TLS：** Rustls with AWS-LC-RS
- **内存分配器：** jemalloc（可选，支持性能分析）

### 开发工具
- **代码风格：** KtLint
- **构建系统：** Cargo NDK（Rust → Android NDK 编译）
- **版本控制：** Git
- **开发环境：** Nix Flakes（可重现构建环境）

## 核心功能

### 1. 配置文件管理
- 支持导入和管理 Clash 配置文件
- 配置文件验证功能
- 多配置文件切换

### 2. VPN 服务
- TUN 模式 VPN 服务
- 前台服务保活机制
- 系统级网络代理
- IPv6 支持
- Fake IP 模式（提升 DNS 解析性能）

### 3. 代理管理
- 代理组可视化管理
- 节点延迟测试
- 智能节点选择
- 代理链路展示

### 4. 流量监控
- 实时流量统计（上传/下载）
- 内存使用监控
- 活跃连接数统计
- 单连接详细信息（来源、代理链、规则匹配）

### 5. 应用过滤
- 应用级别代理控制
- 三种过滤模式：
  - 全部应用通过 VPN
  - 仅选中应用通过 VPN
  - 排除选中应用
- 系统应用过滤
- 应用搜索功能

### 6. 界面与用户体验
- Material Design 3 设计语言
- 深色/浅色主题切换
- 系统主题跟随
- 多语言支持（简体中文、English）
- 响应式 Compose UI
- 流畅的页面导航和动画

### 7. 开发者功能
- 内存分析（支持导出火焰图）
- 详细日志记录
- Tracing 支持（分布式追踪）

## 技术亮点

### 1. 混合语言架构
- **Kotlin 层：** 负责 Android UI、用户交互、系统集成
- **Rust 层：** 负责网络代理核心逻辑、高性能数据处理
- **UniFFI：** 实现 Kotlin 与 Rust 的无缝互操作

### 2. 现代化 Android 开发
- 全面采用 Jetpack Compose 声明式 UI
- 使用 Kotlin Coroutines 进行异步编程
- 遵循 Material Design 3 设计规范
- 采用 MVVM 架构模式

### 3. 高性能网络处理
- Rust 实现核心代理逻辑，保证高性能和内存安全
- 支持多种现代代理协议（TUIC、ShadowQuic、WireGuard）
- 使用 Rustls 提供安全的 TLS 实现
- 异步 I/O（Tokio）保证高并发性能

### 4. 安全性
- 使用 Rustls 替代 OpenSSL，避免内存安全问题
- AWS-LC-RS 加密库支持
- Android VPN Service 权限控制
- 通知权限管理（Android 13+）

### 5. 开发工程化
- Nix Flakes 提供可重现的开发环境
- KtLint 保证代码风格一致性
- Proguard 代码混淆和优化
- 完整的 CI/CD 流程（GitHub Actions）

## 项目架构

```
clash-android/
├── app/                          # Android 应用主模块
│   ├── src/main/
│   │   ├── java/rs/clash/android/
│   │   │   ├── ui/              # Compose UI 组件
│   │   │   ├── service/         # VPN 服务
│   │   │   ├── model/           # 数据模型
│   │   │   └── theme/           # UI 主题
│   │   └── res/                 # Android 资源文件
│   └── build.gradle.kts
├── uniffi/                       # Rust FFI 模块
│   ├── clash-android-ffi/       # Rust 核心实现
│   │   ├── src/
│   │   └── Cargo.toml
│   └── uniffi-bindgen/          # UniFFI 绑定生成
├── core/                         # 核心模块
├── gradle/                       # Gradle 配置
├── build.gradle.kts             # 根 Gradle 配置
└── flake.nix                    # Nix 开发环境配置
```

## 应用截图功能模块

### 主要界面
1. **总览界面（Home）**
   - 显示 VPN 运行状态
   - 实时流量统计
   - 内存使用情况
   - 活跃连接数

2. **代理界面（Panel）**
   - 代理组管理
   - 节点延迟测试
   - 节点选择

3. **配置界面（Profile）**
   - 配置文件导入
   - 配置文件切换
   - 配置验证

4. **设置界面（Settings）**
   - 外观设置（主题、语言）
   - 网络设置（Fake IP、IPv6）
   - 应用过滤
   - 开发者选项

5. **连接界面（Connections）**
   - 实时连接列表
   - 连接详情
   - 流量统计

## 支持的 CPU 架构

- ARM64-v8a (64位 ARM)
- ARMv7 (32位 ARM)
- x86 (32位 x86)
- x86_64 (64位 x86)

## 构建要求

- Android SDK 36
- Android NDK 29.0.14206865
- Android Build Tools 36.0.0
- Rust 工具链（通过 rust-toolchain.toml 自动管理）
- JDK 17+

## 开发团队特点

本项目展示了以下开发能力：

1. **全栈移动开发：** 掌握 Android 原生开发全流程
2. **系统级编程：** 熟悉 Android VPN Service 和网络编程
3. **跨语言开发：** Kotlin + Rust 混合编程经验
4. **现代化 UI 开发：** 精通 Jetpack Compose 和 Material Design
5. **高性能编程：** Rust 异步编程和网络协议实现
6. **开源协作：** 基于 clash-rs 项目进行二次开发
7. **工程化实践：** 完整的构建、测试、发布流程

## 项目成果

- ✅ 完整的 Android VPN 客户端实现
- ✅ 现代化的 Material Design 3 界面
- ✅ 高性能的 Rust 网络核心
- ✅ 多语言支持（中文/英文）
- ✅ 完善的配置管理功能
- ✅ 实时流量和连接监控
- ✅ 灵活的应用过滤机制
- ✅ 开源项目，持续维护更新

## 技术关键词

`Android` `Kotlin` `Rust` `Jetpack Compose` `Material Design 3` `VPN` `Network Proxy` `Clash` `UniFFI` `TUN` `Shadowsocks` `WireGuard` `TUIC` `Async I/O` `Tokio` `Rustls` `NDK` `Gradle` `MVVM` `Coroutines` `FFI`

---

**注：** 本简历基于 Clash Android 开源项目生成，展示了项目的技术栈、功能特性和开发能力。

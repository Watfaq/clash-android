{
  description = "Clash Android 构建环境";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # Android 版本配置
        buildToolsVersion = "36.0.0";
        ndkVersion = "29.0.14206865";
        platformVersion = "36";

        # Android SDK 组合配置
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ buildToolsVersion "35.0.0" ];
          platformVersions = [ platformVersion ];
          abiVersions = [ "x86_64" "arm64-v8a" ];
          ndkVersions = [ ndkVersion ];
          includeNDK = true;
          useGoogleAPIs = false;
          useGoogleTVAddOns = false;
          includeEmulator = false;
          includeSystemImages = false;
          includeSources = false;
        };

        androidSdk = androidComposition.androidsdk;
        jdk = pkgs.jdk21;
        nativeLibraries = [ pkgs.libclang ];

        # NDK 工具链路径
        ndkRoot = "${androidSdk}/libexec/android-sdk/ndk/${ndkVersion}";
        ndkToolchain = "${ndkRoot}/toolchains/llvm/prebuilt/linux-x86_64";
        ndkBin = "${ndkToolchain}/bin";
        ndkSysroot = "${ndkToolchain}/sysroot";

      in
      {
        devShells.default = pkgs.mkShell {
          name = "clash-android-dev";

          buildInputs = with pkgs; [
            # 构建工具
            gradle
            clang
            sccache

            # Rust 工具链
            cargo-ndk

            # Android 开发环境
            androidSdk
            jdk
          ] ++ nativeLibraries;

          # 环境变量配置
          JAVA_HOME = jdk;
          ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
          NDK_HOME = ndkRoot;
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";

          shellHook = ''
            export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath nativeLibraries}:$LD_LIBRARY_PATH
            echo "Clash Android 开发环境已就绪"
            echo "Java: ${jdk.version}"
            echo "Android SDK: ${androidSdk}/libexec/android-sdk"
            echo "NDK: ${ndkVersion}"
            echo "Cargo NDK: 已配置"
          '';
        };
      }
    );
}


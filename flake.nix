{
  description = "Nix Building Environment for Android APP";

  inputs = {
    flake-utils = {
      url = "github:numtide/flake-utils";
    };
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem ( system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk = {
              accept_license = true;
            };
          };
        };
        buildToolsVersion = "35.0.0";
        ndkVersion = "29.0.14206865";
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ buildToolsVersion "34.0.0" ]; # 可以放多个版本
          platformVersions = [ "35" "34" ];
          abiVersions = [ "x86_64" "arm64-v8a" ];
          includeNDK = true;
          useGoogleAPIs = false;
          useGoogleTVAddOns = false;
          includeEmulator = false;
          includeSystemImages = false;
          includeSources = false;
        };
        pinnedJDK = pkgs.jdk21;
        androidSdk = androidComposition.androidsdk;
        libraries = [pkgs.libclang];
      in {
        devShells = {
          default = pkgs.mkShell {
            name = "Android-Build-Shell";
            buildInputs = with pkgs; [
              gradle
              clang

            ] ++ libraries ++ [
              androidSdk
              pinnedJDK
            ];
            JAVA_HOME = pinnedJDK;
            ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
            NDK_HOME = "${androidSdk}/libexec/android-sdk/ndk/${ndkVersion}";
            GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidSdk}/libexec/android-sdk/build-tools/${buildToolsVersion}/aapt2";

          shellHook =
            ''
              export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath libraries}:$LD_LIBRARY_PATH
            '';

          };
        };
      }
    );
}


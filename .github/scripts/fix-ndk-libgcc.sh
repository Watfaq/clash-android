#!/bin/bash

# This script creates libgcc.a redirect files for Android NDK r23+
# where libgcc has been removed in favor of libunwind.
# See: https://github.com/rust-mobile/ndk/issues/149

set -e

if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "Error: ANDROID_NDK_HOME is not set"
    exit 1
fi

echo "Fixing NDK libgcc issue for ANDROID_NDK_HOME=$ANDROID_NDK_HOME"

# Find the clang lib directories with version wildcards
# Note: This script is designed for Linux x86_64 CI runners (ubuntu-latest)
CLANG_BASE_DIR="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/lib"

if [ ! -d "$CLANG_BASE_DIR" ]; then
    echo "Error: Could not find clang lib directory at $CLANG_BASE_DIR"
    exit 1
fi

echo "Found clang lib directory: $CLANG_BASE_DIR"

# Track if we processed any directories
PROCESSED=0

# Create libgcc.a redirect files for each architecture in the clang version directories
for clang_version_dir in "$CLANG_BASE_DIR"/clang/*/lib/linux; do
    if [ -d "$clang_version_dir" ]; then
        echo "Processing clang version directory: $clang_version_dir"
        PROCESSED=1
        
        for arch_dir in "$clang_version_dir"/*; do
            if [ -d "$arch_dir" ]; then
                ARCH=$(basename "$arch_dir")
                LIBGCC_PATH="$arch_dir/libgcc.a"
                
                # Only create if libunwind.a exists and libgcc.a doesn't
                if [ -f "$arch_dir/libunwind.a" ] && [ ! -f "$LIBGCC_PATH" ]; then
                    echo "Creating libgcc.a redirect for $ARCH"
                    echo "INPUT(-lunwind)" > "$LIBGCC_PATH"
                elif [ -f "$LIBGCC_PATH" ]; then
                    echo "libgcc.a already exists for $ARCH, skipping"
                else
                    echo "Warning: libunwind.a not found for $ARCH, skipping"
                fi
            fi
        done
    fi
done

if [ "$PROCESSED" -eq 0 ]; then
    echo "Error: No clang lib directories found. NDK structure may have changed."
    exit 1
fi

echo "NDK libgcc fix completed successfully"

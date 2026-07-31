#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

install_jar() {
    local file="$1"
    local group="$2"
    local artifact="$3"
    local version="$4"

    if [ ! -f "${file}" ]; then
        echo "File not found: ${file}"
        exit 1
    fi

    mvn install:install-file \
        -Dfile="${file}" \
        -DgroupId="${group}" \
        -DartifactId="${artifact}" \
        -Dversion="${version}" \
        -Dpackaging=jar

    echo "Installed ${group}:${artifact}:${version} into the local Maven repository."
}

install_jar "${SCRIPT_DIR}/libs/Slimefun-Build-79809c0a.jar" com.github.servernotdie Slimefun4 79809c0a
install_jar "${SCRIPT_DIR}/libs/FoliaLib-0.5.1.jar" com.tcoded FoliaLib 0.5.1

echo "You can now run: mvn clean package"

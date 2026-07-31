#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PATCH_DIR="${SCRIPT_DIR}"
STATE_FILE="${REPO_ROOT}/.git/souljars-patch.state"

usage() {
    echo "Usage: $0 [apply|revert]"
    echo "  apply   (default) apply all patches from ${PATCH_DIR}"
    echo "  revert  revert all patches, restoring the original source (keeps the patch files)"
    exit 1
}

record_am() {
    echo "am $1 $2 $3" >> "${STATE_FILE}"
}

record_apply() {
    echo "apply $1" >> "${STATE_FILE}"
}

apply_patch() {
    local patch="$1"

    # Prefer git am to keep the original author metadata
    local pre
    pre="$(git rev-parse HEAD)"
    if git am --3way < "${patch}" 2>/dev/null; then
        local post
        post="$(git rev-parse HEAD)"
        if [ "${post}" = "${pre}" ]; then
            echo "Already applied (skip): ${patch}"
            return 0
        fi
        record_am "${patch}" "${pre}" "${post}"
        echo "Applied (git am): ${patch}"
        return 0
    fi
    git am --abort 2>/dev/null || true

    # Fallback: git apply
    if git apply --check --3way "${patch}" 2>/dev/null; then
        git apply --3way "${patch}"
        record_apply "${patch}"
        echo "Applied (git apply): ${patch}"
        return 0
    fi

    # Already applied
    if git apply --reverse --check "${patch}" 2>/dev/null; then
        echo "Already applied (skip): ${patch}"
        return 0
    fi

    echo "ERROR: could not apply ${patch}" >&2
    return 1
}

apply_all() {
    local found=0
    rm -f "${STATE_FILE}"
    for patch in "${PATCH_DIR}"/*.patch; do
        [ -f "${patch}" ] || continue
        found=1
        apply_patch "${patch}"
    done
    [ "${found}" -eq 1 ] || { echo "ERROR: no patch files found in ${PATCH_DIR}" >&2; return 1; }
}

revert_all() {
    local lines=()
    if [ -f "${STATE_FILE}" ]; then
        mapfile -t lines < "${STATE_FILE}"
    fi
    if [ "${#lines[@]}" -eq 0 ]; then
        echo "ERROR: no patch state found (run 'apply' first)" >&2
        return 1
    fi

    local i mode patch pre post head
    for (( i = ${#lines[@]} - 1; i >= 0; i-- )); do
        read -r mode patch pre post <<< "${lines[$i]}"
        head="$(git rev-parse HEAD)"
        if [ "${mode}" = "am" ] && [ "${head}" = "${post}" ] && git diff --quiet && git diff --cached --quiet; then
            git reset --hard -q "${pre}"
            echo "Reverted (removed commit): ${patch}"
        elif git apply -R "${patch}" 2>/dev/null; then
            echo "Reverted: ${patch}"
        elif git apply --reverse --check "${patch}" 2>/dev/null; then
            echo "Not applied (skip): ${patch}"
        else
            echo "ERROR: could not revert ${patch}" >&2
            return 1
        fi
    done
    rm -f "${STATE_FILE}"
}

cd "${REPO_ROOT}"

case "${1:-apply}" in
    apply) apply_all ;;
    revert) revert_all ;;
    *) usage ;;
esac

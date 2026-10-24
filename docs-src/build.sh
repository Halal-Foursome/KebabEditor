#!/usr/bin/env bash
# Builds the site from docs-src/ into docs/ for GitHub Pages (Deploy from a branch: main, /docs).
# docs/ also holds other documents, so only the files recorded in docs/.site-manifest are replaced.
set -euo pipefail

cd "$(dirname "$0")"
docs=../docs
manifest="$docs/.site-manifest"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

zola build --output-dir "$work/site"
touch "$work/site/.nojekyll"

if [ -f "$manifest" ]; then
    while IFS= read -r file; do
        rm -f "$docs/$file"
    done < "$manifest"
fi

(cd "$work/site" && find . -type f | sed 's|^\./||' | sort) > "$work/manifest"
mkdir -p "$docs"
cp -r "$work/site/." "$docs/"
cp "$work/manifest" "$manifest"
find "$docs" -mindepth 1 -type d -empty -delete

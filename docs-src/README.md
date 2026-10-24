# Documentation site

Source of the KebabEditor documentation, built with [Zola](https://www.getzola.org/) and a vendored copy of the
[AdiDoks](https://github.com/aaranxu/adidoks) theme (`themes/adidoks`).

- Pages are markdown files in `content/`.
- `./build.sh` builds the site into `../docs/`, which GitHub Pages serves (Settings > Pages > Deploy from a branch > `main` / `docs`).
  It only replaces files listed in `docs/.site-manifest`, so other documents in `docs/` are left alone.
- Preview while writing: `zola serve` in this folder.
- Built and checked with Zola 0.22.1. Newer Zola versions may need small changes to `config.toml` or the theme templates.

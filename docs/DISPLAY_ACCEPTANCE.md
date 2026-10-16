# Display and settings acceptance (2026-10-16)

- Unit regression: 333 tests passed; subsequent typography/date checks: 31 passed (including the new Auto persistence case).
- TypeScript check and Vite production/PWA build passed.
- Real Chrome layout matrix: 816 combinations, zero viewport/text overflow failures. Covers 12 clock themes, 5 calendar themes, 8 sizes from 320×568 through 3840×2160, Simplified/Traditional Chinese and English, system/Lora fonts, seconds/12-hour/stacked/date-row variants and weather/details/message combinations. The harness checks DOM text and element bounds and uses fixture weather without external service requests.
- Repeat locally with Vite at `/tests/browser-acceptance.html`. This checks the enumerated combinations; it is not a claim about arbitrary third-party fonts or unlimited custom text.
- Browser visual checks: preview-card selection, automatic controls, month picker and settings select styling.
- Motion regression covers repeated rightward swaps including last-to-first wrap; outgoing geometry is frozen before the incoming text is measured.
- Invalid custom date drafts preserve the previous pattern and selection metadata, including when a valid intermediate draft was typed first.

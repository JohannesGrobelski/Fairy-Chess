# Variant Configuration Activity

Allows users to create custom chess variants through a tabbed interface.

## Tabs

### 1. Pieces
- Add/remove pieces
- Upload piece graphics
- Define piece properties (movement, capture patterns)
- Supported formats: PNG, SVG
- Size limits: 512x512px

### 2. Board
- Set board dimensions (1-12 files/ranks)
- Create starting position
- Define special regions:
    - Promotion zones
    - Movement restrictions
    - Valid squares

### 3. Rules
- Basic rules: castling, en passant
- Victory conditions
- Special moves
- Promotion/capture rules

### 4. Validation
- Config preview
- Test variant
- Export settings

## File Structure
- variants.ini
- positionvariants.txt
- variantsettings.txt
- pieces/*.png

## Examples
[code examples for each config file]
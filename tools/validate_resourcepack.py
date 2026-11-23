"""Independent ZIP, dependency, PNG and glyph checks; Python standard library only."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import struct
import sys
import zipfile
import zlib


def png_pixels(data):
    assert data[:8] == b'\x89PNG\r\n\x1a\n', 'Invalid PNG signature'
    offset, compressed, dimensions = 8, bytearray(), None
    while offset < len(data):
        size = struct.unpack_from('>I', data, offset)[0]
        kind, payload = data[offset + 4:offset + 8], data[offset + 8:offset + 8 + size]
        assert zlib.crc32(kind + payload) & 0xffffffff == struct.unpack_from('>I', data, offset + 8 + size)[0], 'PNG CRC'
        if kind == b'IHDR':
            width, height, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', payload)
            assert depth == 8 and color == 6 and compression == filtering == interlace == 0, 'Expected noninterlaced RGBA PNG'
            assert 0 < width <= 1024 and 0 < height <= 1024
            dimensions = width, height
        if kind == b'IDAT':
            compressed.extend(payload)
        offset += size + 12
        if kind == b'IEND':
            break
    assert dimensions and offset == len(data), 'Invalid PNG chunks'
    width, height = dimensions
    raw = zlib.decompress(compressed)
    stride = width * 4
    assert len(raw) == height * (stride + 1), 'PNG decompressed size'
    rows, previous = [], bytearray(stride)
    for y in range(height):
        filtering = raw[y * (stride + 1)]
        row = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        assert 0 <= filtering <= 4
        for x in range(stride):
            left = row[x - 4] if x >= 4 else 0
            up, corner = previous[x], previous[x - 4] if x >= 4 else 0
            prediction = 0
            if filtering == 1:
                prediction = left
            elif filtering == 2:
                prediction = up
            elif filtering == 3:
                prediction = (left + up) // 2
            elif filtering == 4:
                p = left + up - corner
                distances = abs(p - left), abs(p - up), abs(p - corner)
                prediction = (left, up, corner)[distances.index(min(distances))]
            row[x] = (row[x] + prediction) & 255
        rows.append(row)
        previous = row
    return width, height, rows


def main():
    archive = Path(sys.argv[1])
    checks = []
    with zipfile.ZipFile(archive) as pack:
        names = pack.namelist()
        assert len(names) == len(set(names)), 'Duplicate ZIP entries'
        for name in names:
            path = PurePosixPath(name)
            assert not path.is_absolute() and '..' not in path.parts and '\\' not in name
            assert name in ('pack.mcmeta', 'pack.png', 'LICENSE.txt', 'LIESMICH.txt') or name.startswith('assets/kekgames/')
            assert 'private' not in name.lower() and not name.endswith(('.jar', '.class', '.der'))
        assert pack.testzip() is None
        checks.append('ZIP root, CRC, paths, namespace and distribution contents')
        metadata = json.loads(pack.read('pack.mcmeta'))['pack']
        assert metadata['min_format'] == [69, 0] and metadata['max_format'] == [97, 1]
        assert not {'pack_format', 'supported_formats'}.intersection(metadata)
        for version, fmt in {'1.21.10': [69, 0], '1.21.11': [75, 0], '26.1': [84, 0], '26.2': [88, 0], '26.3': [97, 1]}.items():
            assert metadata['min_format'] <= fmt <= metadata['max_format'], version
        checks.append('Pack formats 69.0 through 97.1, including both target endpoints')
        assert png_pixels(pack.read('pack.png'))[:2] == (128, 128)
        checks.append('Pack icon PNG decoding and dimensions')
        fonts = {name.removeprefix('assets/kekgames/font/').removesuffix('.json'): json.loads(pack.read(name))
                 for name in names if name.startswith('assets/kekgames/font/') and name.endswith('.json')}
        assert {'ui', 'heading', 'icons', 'glyphs', 'heading_glyphs'} == set(fonts)
        own_glyphs = {}
        for key, definition in fonts.items():
            assert isinstance(definition.get('providers'), list) and definition['providers']
            glyphs = set()
            for provider in definition['providers']:
                kind = provider['type']
                assert kind in ('space', 'bitmap', 'reference')
                if kind == 'space':
                    assert all(len(char) == 1 and 0 <= advance <= 16 for char, advance in provider['advances'].items())
                    glyphs.update(provider['advances'])
                elif kind == 'reference':
                    assert provider['id'] == 'minecraft:default' or provider['id'].startswith('kekgames:') and provider['id'].split(':')[1] in fonts
                else:
                    assert provider['file'].startswith('kekgames:font/')
                    path = 'assets/kekgames/textures/' + provider['file'].split(':')[1]
                    width, height, pixels = png_pixels(pack.read(path))
                    chars = provider['chars']
                    assert chars and chars[0] and all(len(row) == len(chars[0]) for row in chars)
                    assert width % len(chars[0]) == 0 and height % len(chars) == 0
                    assert (width // len(chars[0]), height // len(chars)) == (8, 8)
                    assert 0 < provider['height'] <= 16 and 0 <= provider['ascent'] <= provider['height']
                    for row, line in enumerate(chars):
                        for column, char in enumerate(line):
                            ink = any(pixels[y][x * 4 + 3] for y in range(row * 8, row * 8 + 8) for x in range(column * 8, column * 8 + 8))
                            if char == '\0':
                                assert not ink, 'Nonempty padding cell'
                            else:
                                assert ink and char not in glyphs, 'Empty or duplicate bitmap glyph'
                                glyphs.add(char)
            own_glyphs[key] = glyphs
        checks.append('All font providers, referenced resources, glyph grids, PNG pixels and baselines')
        def coverage(key, stack=()):
            assert key not in stack, 'Cyclic font dependency'
            result = set(own_glyphs[key])
            for provider in fonts[key]['providers']:
                if provider['type'] == 'reference' and provider['id'].startswith('kekgames:'):
                    result.update(coverage(provider['id'].split(':')[1], stack + (key,)))
            return result
        for key in fonts:
            coverage(key)
        for key in ('ui', 'heading'):
            assert fonts[key]['providers'][-1] == {'type': 'reference', 'id': 'minecraft:default'}
            own = coverage(key)
            assert all(chr(code) in own for code in range(32, 127))
            assert all(char in own for char in 'ÄÖÜäöüßéè€»«•→←↑↓✓✦❤…')
            assert all(chr(code) in own for code in range(0xE000, 0xE010))
            assert set('KekGames » Tägliche Belohnung +400 Coins | Spielzeit: 1h 30min').issubset(own)
        checks.append('ASCII, German characters, symbols, localization samples and Unicode fallback')
        preset = json.loads(Path('resourcepack/preset.json').read_text(encoding='utf-8'))
        assert set(preset) == {'design', 'menuItemTexts'} and preset['design'] == {'font': 'kekgames:ui'}
        assert all(char in coverage('ui') for item in preset['menuItemTexts'].values() for char in item['name'])
        checks.append('Plugin preset references valid font/glyphs and contains no installation secrets')
    data = archive.read_bytes()
    sha1, sha256 = hashlib.sha1(data).hexdigest(), hashlib.sha256(data).hexdigest()
    assert archive.with_suffix('.sha1').read_text().strip() == sha1
    assert archive.with_name('resourcepack-SHA256SUMS.txt').read_text().split()[0] == sha256
    installation = archive.with_name('KekGames-UI-Installation.txt').read_text(encoding='utf-8')
    assert f'resource-pack-sha1={sha1}' in installation and archive.name in installation and 'require-resource-pack=true' in installation
    checks.append('SHA-1/SHA-256 and server installation instructions match the actual ZIP')
    report = {'success': True, 'archive': archive.name, 'bytes': len(data), 'sha1': sha1, 'sha256': sha256,
              'minecraft': ['1.21.10', '26.3'], 'ownGlyphs': len(own_glyphs['glyphs']), 'icons': len(own_glyphs['icons']),
              'checks': checks, 'graphicalClientTest': False}
    archive.with_name('resourcepack-validation.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f"PASS: {len(checks)} resource-pack validation groups, {report['ownGlyphs']} glyphs, {report['icons']} symbols")
    print('SHA-1:', sha1)


if __name__ == '__main__':
    main()

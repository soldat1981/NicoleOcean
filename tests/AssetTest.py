"""Offline resource checks. Run: python tests/AssetTest.py (standard library only)."""
import array
import json
import math
from pathlib import Path
import struct
import sys
import unittest
import wave
import zipfile

ROOT = Path(__file__).resolve().parents[1]


class AssetTest(unittest.TestCase):
    def test_pcm_assets(self):
        paths = sorted((ROOT / 'app/assets/audio').rglob('*.wav'))
        self.assertEqual(len(paths), 30)
        audit = []
        for path in paths:
            with self.subTest(asset=path.name), wave.open(str(path), 'rb') as stream:
                self.assertEqual(stream.getsampwidth(), 2)
                self.assertIn(stream.getnchannels(), (1, 2))
                self.assertGreater(stream.getnframes(), 0)
                samples = array.array('h', stream.readframes(stream.getnframes()))
                if sys.byteorder != 'little':
                    samples.byteswap()
                peak = max(abs(v) for v in samples)
                rms = math.sqrt(sum(v*v for v in samples)/len(samples))/32768
                self.assertLess(peak, 32767, 'Digital clipping')
                self.assertGreater(rms, 0, 'Silent file')
                audit.append(dict(file=path.relative_to(ROOT).as_posix(),
                                  seconds=round(stream.getnframes()/stream.getframerate(), 3),
                                  rate=stream.getframerate(), channels=stream.getnchannels(),
                                  peak=round(peak/32768, 5), rms=round(rms, 5), clipped_samples=0))
        (ROOT/'docs/audio-audit-1.9.json').write_text(
            json.dumps(audit, ensure_ascii=False, indent=2), encoding='utf-8')

    def test_meow_envelopes(self):
        clips=[]
        for name in ('bossi_meow.wav','bossi_meow2.wav','bossi_meow3.wav'):
            with wave.open(str(ROOT/'app/assets/audio'/name),'rb') as sound:
                self.assertEqual(sound.getframerate(),44100)
                samples=array.array('h',sound.readframes(sound.getnframes()))
                if sys.byteorder!='little': samples.byteswap()
                self.assertEqual(samples[0],0)
                self.assertEqual(samples[-1],0)
                self.assertLess(max(abs(v) for v in samples),22000)
                self.assertLess(max(abs(a-b) for a,b in zip(samples,samples[1:])),8000)
                clips.append(samples.tobytes())
        self.assertEqual(len(set(clips)),3)

    def test_music_manifest(self):
        manifest = json.loads((ROOT/'docs/music-manifest.json').read_text(encoding='utf-8'))
        self.assertEqual(len(manifest), 10)
        self.assertEqual(len({entry['file'] for entry in manifest}), 10)
        for entry in manifest:
            with self.subTest(theme=entry['file']), wave.open(str(ROOT/'app/assets/audio/music'/entry['file']), 'rb') as stream:
                self.assertAlmostEqual(stream.getnframes()/stream.getframerate(), entry['seconds'], delta=.02)

    def test_png_headers(self):
        files = list((ROOT/'app/res/drawable-nodpi').glob('*.png'))
        self.assertEqual({f.name for f in files}, {'bossi_atlas.png', 'bossi_looking_down.png', 'bossi_torso.png', 'nicole_faces.png', 'nicole_icon_art.png', 'ocean_ornaments.png', 'home_reef_buildings.png', 'home_reef_decor.png'})
        for file in files:
            with self.subTest(image=file.name):
                header = file.read_bytes()[:33]
                self.assertEqual(header[:8], b'\x89PNG\r\n\x1a\n')
                width, height = struct.unpack('>II', header[16:24])
                self.assertGreater(width, 0)
                self.assertGreater(height, 0)
                self.assertLessEqual(width*height, 8_000_000)
                if file.name.startswith('home_reef_'):
                    self.assertEqual(header[25], 6, 'Home reef sprites preserve RGBA transparency')
                if file.name == 'ocean_ornaments.png':
                    self.assertEqual(width % 3, 0)
                    self.assertEqual(height % 2, 0)
                    self.assertEqual(header[25], 6, 'RGBA atlas preserves transparency')

    def test_packaged_assets(self):
        with zipfile.ZipFile(ROOT/'dist/NicoleOcean.apk') as apk:
            self.assertIsNone(apk.testzip())
            self.assertIn('classes.dex', apk.namelist())
            self.assertTrue(any(name.endswith('/ocean_ornaments.png') for name in apk.namelist()))
            for reef in ('home_reef_buildings.png', 'home_reef_decor.png'):
                self.assertTrue(any(name.endswith('/'+reef) for name in apk.namelist()))
            for name in apk.namelist():
                self.assertNotIn('\\', name)
                self.assertFalse(name.endswith(('.so', '.p12', '.java', '.class')))
                self.assertNotIn('password', name)
            for path in (ROOT/'app/assets').rglob('*'):
                if path.is_file():
                    name = 'assets/' + path.relative_to(ROOT/'app/assets').as_posix()
                    self.assertEqual(apk.read(name), path.read_bytes(), name)
                    self.assertEqual(apk.getinfo(name).compress_type, zipfile.ZIP_STORED,
                                     'MediaPlayer openFd requires uncompressed assets')


if __name__ == '__main__':
    unittest.main(verbosity=2)

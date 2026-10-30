#!/usr/bin/env python3
"""Provision/rotate a household settings key through the existing, permission-protected ADB provider.

Usage: tools/venv/bin/python tools/settings-auth.py --output-dir ~/.cleanyourtv/auth/phone-preview
For a connected TV: add --serial <adb target> --label 'Family TV'.
Desktop-only QR dependency: tools/venv/bin/pip install 'qrcode[pil]'. No launcher dependency.
The output folder must be new, outside the checkout. Scan its QR privately, enter a code on the device,
then delete the folder. A new run with a new folder rotates the old binding; there is no unlock/reset PIN.
"""
import argparse
import base64
import os
from pathlib import Path
import secrets
import shutil
import subprocess
from urllib.parse import quote, urlencode


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default=shutil.which('adb') or str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    parser.add_argument('--serial', help='ADB target; omitted selects the single USB device')
    parser.add_argument('--label', default='Family Launcher Preview')
    parser.add_argument('--output-dir', required=True, type=Path)
    args = parser.parse_args()
    import qrcode  # optional desktop tool, deliberately not bundled with the Android app
    folder = args.output_dir.expanduser().resolve()
    checkout = Path(__file__).resolve().parents[1]
    if folder == checkout or checkout in folder.parents:
        parser.error('Store the private QR outside the checkout.')
    if folder.exists():
        parser.error('Use a new output folder; existing bindings are never overwritten locally.')
    os.umask(0o077)
    folder.mkdir(parents=True, mode=0o700)
    secret = secrets.token_bytes(20)
    encoded = base64.b32encode(secret).decode('ascii')
    uri = 'otpauth://totp/' + quote('Family Launcher:' + args.label, safe='') + '?' + urlencode({
        'secret': encoded, 'issuer': 'Family Launcher', 'algorithm': 'SHA1', 'digits': 6, 'period': 30})
    # No raw secret file, no URI or QR data in logs; the QR is the one temporary enrollment artifact.
    qr = folder / 'authenticator.png'
    qrcode.make(uri).save(qr)
    qr.chmod(0o600)
    target = ['-s', args.serial] if args.serial else ['-d']
    result = subprocess.run([args.adb, *target, 'shell', '-T', 'content', 'write', '--uri',
                             'content://com.gathin.tvhome.theme/settings-auth'],
                            input=secret, capture_output=True, timeout=30)
    output = result.stdout + result.stderr
    if result.returncode or any(word in output for word in (b'Error', b'Exception', b'Unsupported')):
        qr.unlink(missing_ok=True)
        folder.rmdir()
        raise SystemExit('Provisioning failed; check the selected device and installed launcher. No QR retained.')
    print('Private QR saved:', qr)
    print('Scan with Authenticator, then enter its code in Launcher settings to confirm binding.')
    print('Delete this private QR after confirming. Each device should have its own binding.')


if __name__ == '__main__':
    main()

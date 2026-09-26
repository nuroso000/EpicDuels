#!/usr/bin/env python3
"""Compile and execute the standalone regression suites with Maven's resolved test classpath."""
from pathlib import Path
import os
import re
import subprocess
import sys

root = Path(__file__).resolve().parent.parent
flags = ['-o'] if '--offline' in sys.argv else []
result = subprocess.run(['mvn', *flags, '-X', 'test'], cwd=root, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
if result.returncode:
    print(result.stdout)
    sys.exit(result.returncode)
output = re.sub(r'\x1b\[[0-9;]*m', '', result.stdout)
match = re.search(r'\(f\) testPath = \[(.*?)\]', output)
if not match:
    sys.exit('Unable to read Maven compiler testPath; no tests were executed.')
classpath = os.pathsep.join(match.group(1).split(', '))
print('Maven test compilation passed.', flush=True)
for suite in ('PresentationChecks', 'BugAuditChecks'):
    subprocess.run(['java', '-cp', classpath, 'dev.epicduels.' + suite], cwd=root, check=True)

#!/bin/sh
# Downloads Noto Sans Brahmi (SIL Open Font License) into data/fonts/.
# Run once locally and COMMIT the files: F-Droid builds only from what is in the repo.
set -e
cd "$(dirname "$0")/../data/fonts"
BASE=https://raw.githubusercontent.com/google/fonts/main/ofl/notosansbrahmi
curl -fsSL -o NotoSansBrahmi-Regular.ttf "$BASE/NotoSansBrahmi-Regular.ttf"
curl -fsSL -o OFL.txt "$BASE/OFL.txt"
ls -l

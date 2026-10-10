import json
import re
import sys
import zipfile
from collections import Counter
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE.parent.parent / "src" / "client" / "resources" / "assets" / "qualityessentials" / "layout"
LOOM = Path.home() / ".gradle" / "caches" / "fabric-loom"
VERSION = "26.3"
RU_ALPHA = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
EN_ALPHA = "abcdefghijklmnopqrstuvwxyz"
SEED_WEIGHT = 20
MIN_DICT_COUNT = 3


def minecraft_texts():
    with zipfile.ZipFile(LOOM / VERSION / "minecraft-client.jar") as jar:
        english = json.loads(jar.read("assets/minecraft/lang/en_us.json").decode("utf-8"))
    index_file = next((LOOM / "assets" / "indexes").glob(VERSION + "-*.json"))
    objects = json.loads(index_file.read_text(encoding="utf-8"))["objects"]
    digest = objects["minecraft/lang/ru_ru.json"]["hash"]
    russian = json.loads((LOOM / "assets" / "objects" / digest[:2] / digest).read_text(encoding="utf-8"))
    return russian, english


def count_words(texts, alphabet):
    counts = Counter()
    pattern = re.compile("[" + alphabet + "]+")
    for value in texts.values():
        value = re.sub(r"%\d*\$?[sd]", " ", value)
        counts.update(pattern.findall(value.lower()))
    return counts


def seed_words(name, alphabet):
    pattern = re.compile("^[" + alphabet + "]+$")
    words = (HERE / name).read_text(encoding="utf-8").lower().split()
    return [word for word in words if pattern.match(word)]


def grams(counts):
    table = Counter()
    for word, count in counts.items():
        padded = "^^" + word + "$"
        for i in range(2, len(padded)):
            table[padded[i - 1:i + 1]] += count
            table[padded[i - 2:i + 1]] += count
    return table


def build(texts, seed_file, alphabet, name):
    counts = count_words(texts, alphabet)
    seeds = seed_words(seed_file, alphabet)
    training = Counter(counts)
    for word in seeds:
        training[word] += SEED_WEIGHT
    table = grams(training)
    dictionary = {word for word, count in counts.items() if count >= MIN_DICT_COUNT and len(word) > 1}
    dictionary.update(word for word in seeds if len(word) > 1)
    OUT.mkdir(parents=True, exist_ok=True)
    lines = [key + " " + str(value) for key, value in sorted(table.items())]
    (OUT / (name + ".grams")).write_text("\n".join(lines) + "\n", encoding="utf-8")
    (OUT / (name + ".words")).write_text("\n".join(sorted(dictionary)) + "\n", encoding="utf-8")
    return dictionary


def main():
    russian, english = minecraft_texts()
    ru = build(russian, "seed_ru.txt", RU_ALPHA, "ru")
    en = build(english, "seed_en.txt", EN_ALPHA, "en")
    latin = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`"
    cyrillic = "йцукенгшщзхъфывапролджэячсмитьбюё"
    to_cyrillic = str.maketrans(latin, cyrillic)
    clashes = sorted(word for word in en if word.translate(to_cyrillic) in ru)
    print("ru words", len(ru), "en words", len(en), "clashes", clashes)
    return 0


if __name__ == "__main__":
    sys.exit(main())

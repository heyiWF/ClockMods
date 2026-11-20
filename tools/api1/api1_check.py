"""Static API 1 compatibility check for the compiled app classes.

The Android 1.0 verifier rejects a whole class as soon as one of its methods
references a framework member that does not exist on the device, so every
framework call has to be checked, including calls to inherited methods (javac
emits those with the *subclass* as owner, e.g.
``invokevirtual com/clockmods/ui/ColorPickerView.sendAccessibilityEvent``).

Usage: python api1_check.py /path/to/android-1.0/android.jar [--verbose]
Exit code 1 when a missing member is found.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

PROJECT = Path(__file__).resolve().parents[2]
DEFAULT_CLASSES = PROJECT / "app/build/intermediates/javac/release/classes"
JAVA_HOME = os.environ.get("JAVA_HOME")
JAVAP = str(Path(JAVA_HOME) / "bin" / ("javap.exe" if os.name == "nt" else "javap")) \
    if JAVA_HOME else "javap"

METHOD_REF = re.compile(
    r'(?:invokevirtual|invokestatic|invokeinterface|invokespecial)\s+#\d+\s+//\s+'
    r'Method\s+(?:([\w/$]+)\.)?([\w$<>]+):(\S+)')
FIELD_REF = re.compile(
    r'(?:getstatic|putstatic|getfield|putfield)\s+#\d+\s+//\s+'
    r'Field\s+([\w/$]+)\.([\w$]+):(\S+)')

# Classes that exist only on the API level they need and are loaded reflectively:
# SignalStrength/PhoneStateListener (API 7) and View.AccessibilityDelegate (API 14).
ISOLATED = (
    "com/clockmods/ui/MobileSignalMonitor",
    "com/clockmods/platform/AccessibilityCompat",
    "com/clockmods/ui/ColorPickerViewA11y",
)

jar_cache = {}
our_supers = {}


def javap(args):
    result = subprocess.run([JAVAP] + args, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return result.stdout.decode("utf-8", "replace") if result.returncode == 0 else None


def jar_dump(cls):
    if cls in jar_cache:
        return jar_cache[cls]
    variants = [cls]
    parts = cls.split(".")
    for i in range(len(parts) - 1, 1, -1):
        variants.append(".".join(parts[:i]) + "$" + "$".join(parts[i:]))
    found = None
    for variant in variants:
        dump = javap(["-p", "-classpath", JAR, variant])
        if dump is not None:
            found = dump
            break
    jar_cache[cls] = found
    return found


def jar_members(cls, seen=None):
    """Method and field names declared by cls or any of its jar superclasses."""
    if seen is None:
        seen = set()
    if cls in seen:
        return set()
    seen.add(cls)
    dump = jar_dump(cls)
    if dump is None:
        return None
    names = set()
    # Only the class declaration carries the superclass, and generic bounds sit in front of
    # it: "AdapterView<T extends Adapter> extends ViewGroup" would otherwise be read as
    # "extends Adapter", which silently ends the walk at AdapterView and reports every
    # inherited View/TextView method as missing.
    declaration = dump.split("{")[0]
    declaration = re.sub(r'<[^>]*>', '', declaration)
    match = re.search(r'\bextends\s+([\w.$]+)', declaration)
    for line in dump.splitlines():
        line = line.strip()
        if "(" in line:
            name = re.search(r'([\w$<>]+)\s*\(', line)
            if name and ";" in line:
                names.add(name.group(1))
        else:
            name = re.search(r'([\w$]+);\s*$', line)
            if name and not line.startswith("Compiled"):
                names.add(name.group(1))
    if match:
        parent = jar_members(match.group(1).replace("$", "."), seen)
        if parent is None:
            return None
        names |= parent
    return names


def collect():
    refs = []
    files = [os.path.join(dp, n) for dp, _, ns in os.walk(CLASSES) for n in ns
             if n.endswith(".class")]
    for path in files:
        owner = path[len(CLASSES) + 1:].replace("\\", "/")
        if owner.endswith(".class"):
            owner = owner[:-6]
        dump = javap(["-p", "-c", path])
        if dump is None:
            continue
        for match in METHOD_REF.finditer(dump):
            cls = (match.group(1) or owner).replace("/", ".")
            name = match.group(2)
            if name == "<init>":
                continue
            refs.append((cls, name, owner, "method"))
        for match in FIELD_REF.finditer(dump):
            cls, name = match.group(1).replace("/", "."), match.group(2)
            refs.append((cls, name, owner, "field"))
    return refs


def our_super(cls):
    """Superclass of one of our classes, or None."""
    if cls in our_supers:
        return our_supers[cls]
    path = os.path.join(CLASSES, cls.replace(".", os.sep) + ".class")
    parent = None
    dump = javap(["-p", path]) if os.path.exists(path) else None
    if dump:
        match = re.search(r'\bextends\s+([\w.$]+)', dump)
        if match:
            parent = match.group(1).replace("$", ".")
    our_supers[cls] = parent
    return parent


our_members_cache = {}


def our_members(cls):
    if cls in our_members_cache:
        return our_members_cache[cls]
    path = os.path.join(CLASSES, cls.replace(".", os.sep) + ".class")
    names = set()
    dump = javap(["-p", path]) if os.path.exists(path) else None
    if dump:
        for line in dump.splitlines():
            line = line.strip()
            if "(" in line:
                found = re.search(r'([\w$<>]+)\s*\(', line)
                if found and ";" in line:
                    names.add(found.group(1))
            else:
                found = re.search(r'([\w$]+);\s*$', line)
                if found and not line.startswith("Compiled"):
                    names.add(found.group(1))
    our_members_cache[cls] = names
    return names


def declares_ours(cls, name):
    """True when one of our compiled classes declares the member itself."""
    return name in our_members(cls)


def resolve_member(owner, name):
    """Where the member really lives: one of our classes, or a framework class."""
    cls = owner
    for _ in range(20):
        if cls is None:
            return None
        if cls.startswith("android.") or cls.startswith("java."):
            return cls
        if declares_ours(cls, name):
            return None          # defined by us, nothing to check
        cls = our_super(cls)
    return None


def framework_owner(cls):
    """Walk our class hierarchy until a framework class is reached."""
    guard = 0
    while cls and not (cls.startswith("android.") or cls.startswith("java.")):
        cls = our_super(cls)
        guard += 1
        if guard > 20:
            return None
    return cls


def main():
    global JAR, CLASSES
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("android_jar", type=Path)
    parser.add_argument("--classes", type=Path, default=DEFAULT_CLASSES)
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    if not args.android_jar.is_file():
        parser.error(f"Android 1.0 android.jar not found: {args.android_jar}")
    if not any(args.classes.rglob("*.class")):
        parser.error(f"compiled classes not found: {args.classes}")
    JAR = str(args.android_jar.resolve())
    CLASSES = str(args.classes.resolve())
    refs = collect()
    missing = []
    checked = 0
    for cls, name, owner, kind in refs:
        if re.match(r'access\$\d+$', name):
            continue             # javac generated accessor, defined by us
        target = resolve_member(cls, name)
        if target is None or not target.startswith("android."):
            continue
        checked += 1
        names = jar_members(target)
        if names is None:
            missing.append((target, "<class>", owner))
        elif name not in names:
            missing.append((target + "." + name, kind, owner))
    isolated = [m for m in missing if any(m[2].startswith(p) for p in ISOLATED)]
    unique = sorted(m for m in set(missing) if m not in isolated)
    if isolated and args.verbose:
        print("%d references live in API-gated isolated classes (expected)" % len(isolated))
    if args.verbose:
        print("checked %d framework references" % checked)
    if not checked:
        parser.error("no framework references could be checked")
    if not unique:
        print("OK: every framework reference exists on Android 1.0 (%d checked)" % checked)
        return 0
    print("MISSING on Android 1.0 (%d references):" % len(unique))
    for member, kind, owner in unique:
        print("  %-62s %-6s <- %s" % (member, kind, owner))
    return 1


if __name__ == "__main__":
    sys.exit(main())

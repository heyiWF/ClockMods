"""Convert aapt2 resources.arsc UTF-8 string pools to UTF-16 (Android 1.0 compatible).

Android 1.0's ResStringPool only implements the UTF-16 path (no stringAt8), so an
aapt2-built resource tables and compiled XML may contain unreadable UTF-8 pools.
Resource IDs and string indices are preserved.
"""
import struct
import sys

UTF8_FLAG = 0x100
RES_STRING_POOL_TYPE = 0x0001
RES_TABLE_TYPE = 0x0002
RES_XML_TYPE = 0x0003
RES_TABLE_PACKAGE_TYPE = 0x0200


def read_len8(data, pos):
    b = data[pos]
    if b & 0x80:
        return ((b & 0x7F) << 8) | data[pos + 1], pos + 2
    return b, pos + 1


def read_pool_strings(data, off):
    """Return (strings, style_offsets)."""
    typ, hsz, size, scount, stycount, flags, sstart, ystart = struct.unpack_from(
        "<HHIIIIII", data, off)
    assert typ == RES_STRING_POOL_TYPE, "not a string pool"
    offsets = struct.unpack_from("<%dI" % scount, data, off + hsz)
    styles = []
    if stycount:
        styles = list(struct.unpack_from("<%dI" % stycount, data, off + hsz + 4 * scount))
    out = []
    for i in range(scount):
        p = off + sstart + offsets[i]
        if flags & UTF8_FLAG:
            _, p = read_len8(data, p)          # utf16 length (ignored)
            blen, p = read_len8(data, p)       # utf8 byte length
            out.append(data[p:p + blen].decode("utf-8"))
        else:
            # already UTF-16: decode via the 16-bit length prefix
            b0 = struct.unpack_from("<H", data, p)[0]
            p += 2
            if b0 & 0x8000:
                b1 = struct.unpack_from("<H", data, p)[0]
                p += 2
                n = ((b0 & 0x7FFF) << 16) | b1
            else:
                n = b0
            out.append(data[p:p + 2 * n].decode("utf-16-le"))
    return out, styles


def build_pool(strings, style_offsets, style_data):
    """Emit a UTF-16 string pool chunk."""
    encoded = []
    for s in strings:
        raw = s.encode("utf-16-le")
        chars = len(raw) // 2
        if chars > 0x7FFF:
            encoded.append(struct.pack("<H", 0x8000 | (chars >> 16)) +
                           struct.pack("<H", chars & 0xFFFF) + raw + b"\x00\x00")
        else:
            encoded.append(struct.pack("<H", chars) + raw + b"\x00\x00")
    offsets = []
    cursor = 0
    for e in encoded:
        offsets.append(cursor)
        cursor += len(e)
    strings_blob = b"".join(encoded)
    while len(strings_blob) % 4:
        strings_blob += b"\x00"

    hsz = 28
    strings_start = hsz + 4 * len(strings) + 4 * len(style_offsets)
    styles_start = 0
    styles_blob = b""
    if style_offsets:
        styles_start = strings_start + len(strings_blob)
        styles_blob = style_data
        while len(styles_blob) % 4:
            styles_blob += b"\x00"

    size = strings_start + len(strings_blob) + len(styles_blob)
    header = struct.pack("<HHIIIIII", RES_STRING_POOL_TYPE, hsz, size,
                         len(strings), len(style_offsets), 0, strings_start, styles_start)
    off_table = struct.pack("<%dI" % len(offsets), *offsets) if offsets else b""
    style_table = struct.pack("<%dI" % len(style_offsets), *style_offsets) if style_offsets else b""
    return header + off_table + style_table + strings_blob + styles_blob


def convert_pool_chunk(data, off):
    typ, hsz, size = struct.unpack_from("<HHI", data, off)
    _, _, _, scount, stycount, flags, _, ystart = struct.unpack_from("<HHIIIIII", data, off)
    strings, style_offsets = read_pool_strings(data, off)
    style_data = b""
    if stycount and ystart:
        # Style spans are index based; length is scount*8 terminating spans.
        p = off + ystart
        end = off + size
        # Copy raw style bytes (already index based) up to chunk end.
        style_data = data[p:end]
    return build_pool(strings, style_offsets, style_data)


def convert_package(data, off):
    typ, hsz, size = struct.unpack_from("<HHI", data, off)
    assert typ == RES_TABLE_PACKAGE_TYPE
    header = bytearray(data[off:off + hsz])
    type_strings_off, last_public_type, key_strings_off, last_public_key = struct.unpack_from(
        "<IIII", data, off + 268)

    pos = off + hsz
    new_pool_pos = {}
    body = b""
    rel = hsz  # running offset relative to package start
    while pos < off + size:
        ctyp, chsz, csize = struct.unpack_from("<HHI", data, pos)
        if ctyp == RES_STRING_POOL_TYPE:
            chunk = convert_pool_chunk(data, pos)
            if pos == off + type_strings_off:
                new_pool_pos["type"] = rel
            if pos == off + key_strings_off:
                new_pool_pos["key"] = rel
        else:
            chunk = data[pos:pos + csize]
        body += chunk
        rel += len(chunk)
        pos += csize

    new_type_off = new_pool_pos.get("type", type_strings_off)
    new_key_off = new_pool_pos.get("key", key_strings_off)
    struct.pack_into("<IIII", header, 268, new_type_off, last_public_type,
                     new_key_off, last_public_key)
    new_size = hsz + len(body)
    struct.pack_into("<HHI", header, 0, RES_TABLE_PACKAGE_TYPE, hsz, new_size)
    return bytes(header) + body


def convert_arsc(data):
    typ, hsz, size, pkg_count = struct.unpack_from("<HHII", data, 0)
    assert typ == RES_TABLE_TYPE, "not a resource table"
    pos = hsz
    body = b""
    while pos < size:
        ctyp, chsz, csize = struct.unpack_from("<HHI", data, pos)
        if ctyp == RES_STRING_POOL_TYPE:
            chunk = convert_pool_chunk(data, pos)
        elif ctyp == RES_TABLE_PACKAGE_TYPE:
            chunk = convert_package(data, pos)
        else:
            chunk = data[pos:pos + csize]
        body += chunk
        pos += csize
    header = struct.pack("<HHI", RES_TABLE_TYPE, hsz, hsz + len(body))
    # packageCount follows the size field
    header += struct.pack("<I", pkg_count)
    return header + body

def convert_xml(data):
    """Rewrite UTF-8 string pools inside a compiled XML file."""
    typ, hsz, size = struct.unpack_from("<HHI", data, 0)
    if typ != RES_XML_TYPE:
        return data
    pos = hsz
    body = b""
    changed = False
    while pos < size:
        ctyp, chsz, csize = struct.unpack_from("<HHI", data, pos)
        chunk = data[pos:pos + csize]
        if ctyp == RES_STRING_POOL_TYPE:
            flags = struct.unpack_from("<I", data, pos + 16)[0]
            if flags & UTF8_FLAG:
                chunk = convert_pool_chunk(data, pos)
                changed = True
        body += chunk
        pos += csize
    if not changed:
        return data
    return struct.pack("<HHI", RES_XML_TYPE, hsz, hsz + len(body)) + body


if __name__ == "__main__":
    with open(sys.argv[1], "rb") as f:
        src = f.read()
    out = convert_arsc(src)
    with open(sys.argv[2], "wb") as f:
        f.write(out)
    print("converted %d -> %d bytes" % (len(src), len(out)))

package br.com.lfmelo.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.exception.BadRequestException;

final class EvidenceImageValidator {
    private static final Set<String> HEIC_BRANDS = Set.of("heic", "heix", "hevc", "hevx", "heim", "heis");
    private static final long MAX_PIXELS = 25_000_000L;

    private EvidenceImageValidator() {
    }

    static void validate(Path path, EvidenceContentType type) throws IOException {
        boolean valid;
        try {
            valid = switch (type) {
                case IMAGE_JPEG -> decode(path, "JPEG");
                case IMAGE_PNG -> decode(path, "PNG");
                case IMAGE_WEBP -> webp(path);
                case IMAGE_HEIC -> heic(path);
            };
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            valid = false;
        }
        if (!valid) {
            throw new BadRequestException("Evidence content does not match a valid supported image");
        }
    }

    private static boolean decode(Path path, String format) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(path.toFile())) {
            if (stream == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                return format.equalsIgnoreCase(reader.getFormatName()) && width > 0 && height > 0
                        && width * height <= MAX_PIXELS && reader.read(0) != null;
            } catch (IIOException ex) {
                return false;
            } finally {
                reader.dispose();
            }
        }
    }

    private static boolean webp(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long length = file.length();
            if (length < 20 || !fourcc(file).equals("RIFF") || little32(file) + 8 != length
                    || !fourcc(file).equals("WEBP")) {
                return false;
            }
            boolean image = false;
            while (file.getFilePointer() < length) {
                if (length - file.getFilePointer() < 8) {
                    return false;
                }
                String chunk = fourcc(file);
                long size = little32(file);
                long start = file.getFilePointer();
                long padded = size + (size & 1);
                if (padded > length - start) {
                    return false;
                }
                if (chunk.equals("VP8 ") || chunk.equals("VP8L")) {
                    image |= validWebpFrame(file, chunk, size);
                } else if (chunk.equals("ANMF") && size >= 24) {
                    file.seek(start + 16);
                    long end = start + size;
                    while (file.getFilePointer() < end) {
                        if (end - file.getFilePointer() < 8) {
                            return false;
                        }
                        String frameType = fourcc(file);
                        long frameSize = little32(file);
                        long frameStart = file.getFilePointer();
                        long framePadded = frameSize + (frameSize & 1);
                        if (framePadded > end - frameStart) {
                            return false;
                        }
                        image |= validWebpFrame(file, frameType, frameSize);
                        file.seek(frameStart + framePadded);
                    }
                }
                file.seek(start + padded);
            }

            return image;
        }
    }

    private static boolean validWebpFrame(RandomAccessFile file, String type, long size) throws IOException {
        if (type.equals("VP8 ") && size > 10) {
            int frame = file.readUnsignedByte();
            file.skipBytes(2);
            if ((frame & 1) != 0 || file.readUnsignedByte() != 0x9d
                    || file.readUnsignedByte() != 0x01 || file.readUnsignedByte() != 0x2a) {
                return false;
            }
            int width = (file.readUnsignedByte() | file.readUnsignedByte() << 8) & 0x3fff;
            int height = (file.readUnsignedByte() | file.readUnsignedByte() << 8) & 0x3fff;
            return width > 0 && height > 0 && (long) width * height <= MAX_PIXELS;
        }
        if (type.equals("VP8L") && size > 5 && file.readUnsignedByte() == 0x2f) {
            long bits = little32(file);
            long width = (bits & 0x3fff) + 1;
            long height = ((bits >> 14) & 0x3fff) + 1;
            return (bits >> 29) == 0 && width * height <= MAX_PIXELS;
        }
        return false;
    }

    private static boolean heic(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long length = file.length();
            boolean brand = false;
            boolean metadata = false;
            boolean data = false;
            while (file.getFilePointer() < length) {
                long start = file.getFilePointer();
                if (length - start < 8) {
                    return false;
                }
                long size = Integer.toUnsignedLong(file.readInt());
                String box = fourcc(file);
                long header = 8;
                if (size == 1) {
                    if (length - start < 16) {
                        return false;
                    }
                    size = file.readLong();
                    header = 16;
                } else if (size == 0) {
                    size = length - start;
                }
                if (size < header || size > length - start) {
                    return false;
                }
                if (box.equals("ftyp")) {
                    if (start != 0 || size < header + 8 || (size - header - 8) % 4 != 0) {
                        return false;
                    }
                    brand = HEIC_BRANDS.contains(fourcc(file));
                    file.skipBytes(4);
                    while (file.getFilePointer() < start + size) {
                        brand |= HEIC_BRANDS.contains(fourcc(file));
                    }
                } else if (box.equals("meta")) {
                    metadata = validHeicMetadata(file, start + size, header, size);
                } else if (box.equals("mdat")) {
                    data = size > header;
                }

                file.seek(start + size);
            }
            return brand && metadata && data;
        }
    }

    private static boolean validHeicMetadata(RandomAccessFile file, long end, long header, long size)
            throws IOException {
        if (size < header + 4) {
            return false;
        }
        file.skipBytes(4);
        boolean handler = false;
        boolean primaryItem = false;
        boolean itemInfo = false;
        boolean itemLocation = false;
        while (file.getFilePointer() < end) {
            long start = file.getFilePointer();
            if (end - start < 8) {
                return false;
            }
            long childSize = Integer.toUnsignedLong(file.readInt());
            String child = fourcc(file);
            long childHeader = 8;
            if (childSize == 1) {
                if (end - start < 16) {
                    return false;
                }
                childSize = file.readLong();
                childHeader = 16;
            } else if (childSize == 0) {
                childSize = end - start;
            }
            if (childSize < childHeader || childSize > end - start) {
                return false;
            }
            if (child.equals("hdlr") && childSize >= childHeader + 12) {
                file.skipBytes(8);
                handler = fourcc(file).equals("pict");
            } else if (child.equals("pitm")) {
                primaryItem = childSize >= childHeader + 6;
            } else if (child.equals("iinf")) {
                itemInfo = childSize >= childHeader + 6;
            } else if (child.equals("iloc")) {
                itemLocation = childSize >= childHeader + 8;
            }
            file.seek(start + childSize);
        }
        return handler && primaryItem && itemInfo && itemLocation;
    }

    private static String fourcc(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[4];
        file.readFully(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private static long little32(RandomAccessFile file) throws IOException {
        return Integer.toUnsignedLong(Integer.reverseBytes(file.readInt()));
    }
}

import os
import glob
from shutil import copy2, move, SameFileError
from tempfile import mkstemp

root = os.path.dirname(os.path.realpath(__file__))
headers_hpp = glob.glob(root + '/**/*.hpp', recursive=True)
headers_h = glob.glob(root + '/**/*.h', recursive=True)
sources_cpp = glob.glob(root + '/**/*.cpp', recursive=True)
sources_c = glob.glob(root + '/**/*.c', recursive=True)
sources = sources_cpp + sources_c + headers_hpp + headers_h

moved_files = []
for s in sources:
    try:
        new_path = os.path.join(root, os.path.basename(s))
        copy2(s, new_path)
        moved_files.append(new_path)
    except SameFileError:
        pass

for file in moved_files:
    fh, abs_path = mkstemp()
    with os.fdopen(fh, 'w') as new_file:
        with open(file, encoding='utf-8', errors='ignore') as old_file:
            for line in old_file:
                new_line = line
                if line.startswith("#include"):
                    s_line1 = line.split()
                    if len(s_line1) == 2:
                        s_line2 = s_line1[1].split('"')
                        if len(s_line2) == 3:
                            s_line3 = s_line2[1].split("/")
                            if len(s_line3) > 1:
                                new_line = s_line1[0]
                                new_line += ' "'
                                new_line += s_line3[-1]
                                new_line += '"'
                                new_line += "\r\n"
                                print(new_line, end="")
                new_file.write(new_line)
    os.remove(file)
    move(abs_path, file)


print("Please compile now and then press enter")
input()

for file in moved_files:
    os.remove(file)

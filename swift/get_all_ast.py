#!/usr/bin/env python

import subprocess
import os
import json
BASE = '/Users/jared/khan/iOS'
black = []
OUT = './ast'
EXT = '.ast'

config = json.load(open('./config.json'))

def prefix(items, prefix):
    for item in items:
        yield prefix
        yield item

def get_args(include, frameworks, flags, ccflags, etc, **args):
    res = ['-v']
    for k, v in args.items():
        res += ["-" + k, v]
    res += prefix(include, '-I')
    res += prefix(frameworks, '-F')
    res += prefix(flags, '-D')
    res += prefix(ccflags, '-Xcc')
    res += etc
    return res

def get_command(filename, swiftc, **args):
    return [swiftc, filename] + get_args(**args)

def get_allcommand(filenames, swiftc, **args):
    return [swiftc] + filenames + get_args(**args)

def process(filename):
    print "processing", filename
    folder = os.path.dirname(filename)
    relfolder = os.path.relpath(folder, BASE)
    newfolder = os.path.join(OUT, relfolder)
    if not os.path.exists(newfolder):
        os.makedirs(newfolder)
    newname = os.path.splitext(os.path.basename(filename))[0] + EXT
    outpath = os.path.join(newfolder, newname)
    if os.path.exists(outpath):
        return

    return filename

def process_all(to_process):
    filelist = os.path.join(BASE, 'filelist')
    open(filelist, 'w').write('\n'.join(to_process))
    for filename in to_process:
        try:
            raw_output = subprocess.check_output(
                get_command(filename, **config),
                stderr=subprocess.STDOUT,
                cwd=BASE
            )
        except subprocess.CalledProcessError as e:
            print "Failed to process {} :( :(".format(filename)
            open('./error.log', 'w').write(e.output)
            fail
            return

        open(outpath, 'w').write(raw_output)


def process_atonce(to_process):
    print to_process[0], "first one"
    open('./files','w').write('\n'.join(to_process))
    try:
        raw_output = subprocess.check_output(
            get_allcommand(to_process, **config),
            stderr=subprocess.STDOUT,
            cwd=BASE
        )
    except subprocess.CalledProcessError as e:
        print "Failed to process all :( :("
        open('./error.log', 'w').write(e.output)
        fail
        return

    open('./output.log', 'w').write(raw_output)

# process('/Users/jared/khan/iOS/Model/ContentType.swift')

blacklist = [
        'Carthage',
        'KHACore',
        'ThirdParty',
        'Build',
        'node_modules',
]

def walk(directory, to_process):
    print "walking", directory
    for name in os.listdir(directory):
        if name[0] == '.': continue
        if name in blacklist: continue
        if name.endswith('.framework'): continue
        if name.endswith('.xcworkspace'): continue
        if name.endswith('.xcodeproj'): continue

        full = os.path.join(directory, name)
        if name.endswith('.swift'):
            dest = process(full)
            if dest:
                to_process.append(dest)
        elif os.path.isdir(full):
            walk(full, to_process)


# to_process = []
# walk(BASE, to_process)
data = json.load(open('./main-files.json'))
print len(data)
print data[:10]
to_process = [os.path.join(BASE, x) for x in data if x and x.endswith('.swift')]
process_atonce(to_process)

print "DONE"
